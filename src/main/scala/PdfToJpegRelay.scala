package pdftojpg

import org.slf4j.{Logger, LoggerFactory}

import scala.util.control.NonFatal
import scalaz.concurrent.{Task, TaskApp}
import scalaz.{-\/, \/}

object PdfToJpegRelay extends TaskApp {
  import Attachments._
  import EmailConfig._
  import FileConversion._
  import MessageConversion._
  import Pop3._
  import Smtp._
  import TaskUtils._

  // Thanks to
  // https://www.athlinks.com/engineering/blog/2017/02/09/how-to-write-an-infinite-loop-using-scalaz-tasks-for-asynchronous-cancellable-jobs/
  // for helping me learn how to write recurring Tasks.
  override def runl(args: List[String]): Task[Unit] = {
    Task.tailrecM[Unit, Unit](_ => checkMail)(())
  }

  private[this] def checkMail: Task[\/[Unit, Unit]] = {
    val t: Task[\/[Unit, Unit]] = for {
      ctx <- createContext()
      allMessages <- getEmails.run(ctx)
      messagesWithPdfs <- findMessagesWithPdfs(allMessages).run(ctx)
      messagesForRSMWithImages <- transformMessages(messagesWithPdfs).run(ctx)
      sentMessages <- sendMessages(messagesForRSMWithImages).run(ctx)
      cleanedUp <- cleanupFiles(sentMessages).run(ctx)
    } yield {
      ctx.logger.info("Waiting")
      Thread.sleep(ctx.config.pollingPeriodSeconds * 1000)
      -\/(())
    }

    t.handle[\/[Unit, Unit]] {
      case NonFatal(e) =>
        println("Oops, error occurred:")
        println(e.getMessage)
        e.printStackTrace()
        -\/(())
    }
  }

  private[this] def createContext(): Task[Context] =
    for {
      config <- loadConfig
      logger <- createLogger()
    } yield {
      Context(config, logger)
    }

  private[this] def createLogger(): Task[Logger] = taskFromUnsafe {
    LoggerFactory.getLogger("pdftojpeg.Poller")
  }
}