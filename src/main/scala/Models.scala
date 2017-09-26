package pdftojpg

import java.io.File
import javax.mail.internet.MimeMessage

object Models {
  case class SavedPdf(file: File) {
    def baseName: String = {
      file.getName.replaceAll("\\.pdf", "").replaceAll("\\.PDF", "")
    }
  }

  case class TempFolder(folder: File)

  case class SavedPageImage(file: File)

  case class MessageWithPdf(mimeMessage: MimeMessage, pdf: SavedPdf, folder: TempFolder)

  case class MessageAndImages(mimeMessage: MimeMessage, images: List[SavedPageImage], folder: TempFolder)

  case class MessageWithAttachedImages(mimeMessage: MimeMessage, tempFolder: TempFolder)

  case class SentMessage(messageWithAttachedImages: MessageWithAttachedImages)

  case class DeletedFile(file: File)
}