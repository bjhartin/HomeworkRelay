package pdftojpg

import javax.mail._
import javax.mail.internet.MimeMessage
import javax.mail.search.FlagTerm

import com.sun.mail.pop3.POP3SSLStore
import pdftojpg.TaskUtils.taskFromUnsafe

import scalaz.Kleisli
import scalaz.concurrent.Task

object Pop3 extends Pop3

trait Pop3 {
  def getEmails: Kleisli[Task, Context, List[MimeMessage]] =
    for {
      store <- connectToPop3Store()
      inbox <- openInbox(store)
      allMessages <- getMessages(inbox)
    } yield {
      allMessages
    }


  private[this] def connectToPop3Store(): Kleisli[Task, Context, Store] = Kleisli { ctx =>
    taskFromUnsafe {
      import ctx.config._
      import ctx.config.pop3._

      ctx.logger.info("Connecting to pop3 store")
      val url = new URLName("pop3", host, port, "", username, password)
      val session = Session.getInstance(sslPop3Properties, null)
      val store = new POP3SSLStore(session, url)
      store.connect()
      store
    }
  }

  private[this] def openInbox(store: Store): Kleisli[Task, Context, Folder] = Kleisli { ctx =>
    taskFromUnsafe {
      ctx.logger.info("Opening inbox")
      val inbox: Folder = store.getFolder("Inbox")
      inbox.open(Folder.READ_ONLY)
      inbox
    }
  }

  private[this] def getMessages(folder: Folder): Kleisli[Task, Context, List[MimeMessage]] = Kleisli { ctx =>
    taskFromUnsafe {
      ctx.logger.info("Getting messages")
      val seenFlag = new Flags(Flags.Flag.SEEN)
      val unseenFlagTerm: FlagTerm = new FlagTerm(seenFlag, false)

      folder.search(unseenFlagTerm).toList.asInstanceOf[List[MimeMessage]]
    }
  }
}
