package pdftojpg

import pdftojpg.FileConversion.pdfPagesToImages

import scalaz.concurrent.Task
import scalaz.{Kleisli, ListT}

object MessageConversion extends MessageConversion

trait MessageConversion {
  import Models._
  import Attachments._
  import Smtp._

  def transformMessages(messagesWithPdf: List[MessageWithPdf]): Kleisli[Task, Context, List[MessageWithAttachedImages]] = Kleisli { ctx =>
    val tasksT = for {
      messageAndAttachment <- ListT(Task.delay(messagesWithPdf))
      messageAndImages <- ListT(transformMessage(messageAndAttachment).map(List(_)).run(ctx))
    } yield {

      messageAndImages
    }
    tasksT.run
  }

  def transformMessage(message: MessageWithPdf): Kleisli[Task, Context, MessageWithAttachedImages] = Kleisli { ctx =>
    for {
      images <- pdfPagesToImages(message.pdf).run(ctx)
      newMessage <- createRsmMessage(message.mimeMessage).run(ctx)
      messageWithAttachedImages <- attachImages(newMessage, images, message.folder).run(ctx)
    } yield {
      messageWithAttachedImages
    }
  }
}
