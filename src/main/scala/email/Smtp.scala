package pdftojpg

import javax.mail._
import javax.mail.internet.MimeMessage

import pdftojpg.TaskUtils.taskFromUnsafe

import scala.util.control.NoStackTrace
import scalaz.Kleisli
import scalaz.concurrent.Task

import scalaz._, Scalaz._

object Smtp extends Smtp

trait Smtp {
  def createRsmMessage(original: MimeMessage): Kleisli[Task, Context, MimeMessage] =
    for {
      session <- openSession
      mimeMessage <- createMimeMessage(original, session)
    } yield {
      mimeMessage
    }

  def sendMessages(messages: List[MimeMessage]): Kleisli[Task, Context, List[Unit]] =
    messages.traverseU(m => sendMessage(m))

  def sendMessage(message: MimeMessage): Kleisli[Task, Context, Unit] = Kleisli { ctx =>
    taskFromUnsafe {
      ctx.logger.info(s"Sending message: ${message.getSubject}")
      Transport.send(message)
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

      val message = new MimeMessage(session)

      ctx.logger.info(s"Creating new message from original: ${original.getSubject}")

      val from = original.getFrom.toList.headOption.getOrElse(throw new NoSender(original.getSubject))

      message.setFrom(from)
      message.setSender(original.getSender)
      message.setRecipients(Message.RecipientType.TO, toAddress)
      message.setSubject(subjectPrefix)
      message.setText(body)

      ctx.logger.info(s"New message: ${message.getSubject} from $from")
      message
    }
  }

  case class NoSender(subject: String) extends NoStackTrace
}