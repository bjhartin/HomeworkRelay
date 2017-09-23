package pdftojpg

import java.io.File
import java.util.UUID
import javax.activation.{DataHandler, DataSource, FileDataSource}
import javax.mail.internet.{MimeBodyPart, MimeMessage, MimeMultipart}
import javax.mail.{BodyPart, Multipart, Part}

import scalaz.{Kleisli, ListT}
import scalaz.concurrent.Task

object Attachments extends Attachments {
  object Models {
    case object MultipartMessage {
      def unapply(m: MimeMessage): Option[Multipart] = {
        m.getContent match {
          case mp:Multipart => Some(mp)
          case _ => None
        }
      }
    }

    case class PdfAttachment(part: MimeBodyPart)

    object PdfAttachment {
      def unapply(bp: BodyPart): Option[PdfAttachment] = {
        bp match {
          case mbp: MimeBodyPart if Part.ATTACHMENT.equalsIgnoreCase(mbp.getDisposition) =>
            val filename = mbp.getFileName
            if (filename.toUpperCase.endsWith(".PDF"))
              Some(PdfAttachment(mbp))
            else
              None
          case _ => None
        }
      }
    }
  }
}

trait Attachments {
  import Attachments.Models._
  import pdftojpg.Models._
  import pdftojpg.TaskUtils._

  // TODO: We probably want this to take a domain type and return one, e.g.
  // MessageAndImages and MessageWithImagesAttached
  def attachImages(message: MimeMessage, images: List[SavedPageImage]): Kleisli[Task, Context, MessageWithAttachedImages] = {
    for {
      multipart <- toMultipart(images)
    } yield {
      message.setContent(multipart)
      MessageWithAttachedImages(message)
    }
  }

  def findMessagesWithPdfs(messages: List[MimeMessage]): Kleisli[Task, Context, List[MessageWithPdf]] = Kleisli { ctx =>
    val m = for {
      message <- ListT(Task.delay(messages))
      attachment <- ListT(getAttachmentFromMessage(message).map(_.toList).run(ctx))
      pdf <- ListT(savePdfAttachment(attachment).map(List(_)).run(ctx))
    } yield {
      MessageWithPdf(message, pdf)
    }
    m.run
  }

  private[this] def getAttachmentFromMessage(message: MimeMessage): Kleisli[Task, Context, Option[PdfAttachment]] =
    message match {
      case MultipartMessage(multipart) => getAttachmentFromMultipart(multipart)
      case _ => Kleisli { ctx => Task.delay(None)}
    }

  private[this] def getAttachmentFromMultipart(m: Multipart): Kleisli[Task, Context, Option[PdfAttachment]] = Kleisli { ctx =>
    taskFromUnsafe {
      val range = 0.until(m.getCount)
      range.foldLeft(Option.empty[PdfAttachment]) { (acc, i) =>
        val part = m.getBodyPart(i)
        (acc, part) match {
          case (Some(_), _) => acc
          case (None, PdfAttachment(pdf)) => Some(pdf)
          case _ => None
        }
      }
    }
  }

  private[this] def savePdfAttachment(pdf: PdfAttachment): Kleisli[Task, Context, SavedPdf] = Kleisli { ctx =>
    taskFromUnsafe {
      val part = pdf.part
      val uuid = UUID.randomUUID()
      val filename = s"$uuid-${part.getFileName}"
      ctx.logger.info(s"Saving PDF attachment: $filename")
      part.saveFile(filename)
      SavedPdf(new File(filename))
    }
  }

  private[this] def toMultipart(images: List[SavedPageImage]): Kleisli[Task, Context, Multipart] = Kleisli { ctx =>
    taskFromUnsafe {
      val files = images.map(_.file)
      val multipart: Multipart = new MimeMultipart()

      files.foreach { f =>
        val source: DataSource = new FileDataSource(f)

        val messageBodyPart: MimeBodyPart = new MimeBodyPart()

        messageBodyPart.setDataHandler(new DataHandler(source))
        messageBodyPart.setFileName(f.getAbsolutePath)

        multipart.addBodyPart(messageBodyPart)
      }

      multipart
    }
  }
}
