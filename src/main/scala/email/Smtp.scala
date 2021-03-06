package pdftojpg

import javax.mail._
import javax.mail.internet.MimeMessage

import pdftojpg.Models.{MessageWithAttachedImages, SentMessage}
import pdftojpg.TaskUtils.taskFromUnsafe

import scala.util.control.NoStackTrace
import scalaz.Scalaz._
import scalaz.{Kleisli, _}
import scalaz.concurrent.Task

object Smtp extends Smtp

trait Smtp {
  def createRsmMessage(original: MimeMessage): Kleisli[Task, Context, MimeMessage] =
    for {
      session <- openSession
      mimeMessage <- createMimeMessage(original, session)
    } yield {
      mimeMessage
    }

  def sendMessages(messages: List[MessageWithAttachedImages]): Kleisli[Task, Context, List[SentMessage]] =
    messages.traverseU(m => sendMessage(m))

  def sendMessage(message: MessageWithAttachedImages): Kleisli[Task, Context, SentMessage] = Kleisli { ctx =>
    taskFromUnsafe {
      ctx.logger.info(s"Sending message: ${message.mimeMessage.getSubject}")
      //Transport.send(message.mimeMessage)
      SentMessage(message)
    }
  }

  private[this] def openSession: Kleisli[Task, Context, Session] = Kleisli { ctx =>
    taskFromUnsafe {
      import ctx.config._

      Session.getInstance(smtpProperties,
        new javax.mail.Authenticator() {
          override def getPasswordAuthentication =
            new PasswordAuthentication(username, password)
        })
    }
  }

  private[this] def createMimeMessage(original: MimeMessage, session: Session): Kleisli[Task, Context, MimeMessage] = Kleisli { ctx =>
    taskFromUnsafe {
      import ctx.config.rsmMessage._

      val newMessage = new MimeMessage(session)

      ctx.logger.info(s"Creating new message from original: ${original.getSubject}")

      val from = original.getFrom.toList.headOption.getOrElse(throw new NoSender(original.getSubject))

      newMessage.setFrom(from)
      newMessage.setSender(original.getSender)
      newMessage.setRecipients(Message.RecipientType.TO, toAddress)
      newMessage.setRecipients(Message.RecipientType.CC, ccAddress)
      newMessage.setSubject(s"Homework files from $from")
      newMessage.setText(body)

      ctx.logger.info(s"New message: ${newMessage.getSubject}")
      newMessage
    }
  }

  case class NoSender(subject: String) extends NoStackTrace
}
