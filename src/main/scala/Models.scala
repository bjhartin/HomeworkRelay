package pdftojpg

import java.io.File
import javax.mail.internet.MimeMessage

object Models {
  case class SavedPdf(file: File) {
    def baseName: String = {
      file.getName.replaceAll("\\.pdf", "").replaceAll("\\.PDF", "")
    }
  }
  case class SavedPageImage(file: File)

  case class MessageWithPdf(message: MimeMessage, pdf: SavedPdf)

  case class MessageAndImages(message: MimeMessage, images: List[SavedPageImage])

  case class MessageWithAttachedImages(message: MimeMessage)

  case class SentMessage(message: MessageWithAttachedImages)
}