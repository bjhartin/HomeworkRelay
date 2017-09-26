package pdftojpg

import java.awt.image.RenderedImage
import java.io.File
import javax.imageio.ImageIO

import org.ghost4j.document.{Document, PDFDocument}
import org.ghost4j.renderer.SimpleRenderer

import scala.collection.JavaConversions._
import scala.util.control.NoStackTrace
import scalaz.Kleisli
import scalaz.concurrent.Task

object FileConversion extends FileConversion

trait FileConversion {
  import Models._
  import TaskUtils._

  def pdfPagesToImages(pdf: SavedPdf): Kleisli[Task, Context, List[SavedPageImage]] = {
    for {
      pdfDocument <- loadDoc(pdf)
      imageRenderer <- makeRenderer()
      renderedImages <- renderImages(imageRenderer, pdfDocument)
      savedImages <- saveImages(renderedImages, pdf)
    } yield {
      savedImages
    }
  }

  def cleanupFiles(sentMessages: List[SentMessage]): Kleisli[Task, Context, List[DeletedFile]] = Kleisli { ctx =>
    val tasks = sentMessages.map {m =>
      ctx.logger.info(s"Deleting folder ${m.messageWithAttachedImages.tempFolder.folder.getAbsolutePath}")
      delete(m.messageWithAttachedImages.tempFolder.folder)
    }
    Task.gatherUnordered(tasks)
  }

  private[this] def loadDoc(pdf: SavedPdf): Kleisli[Task, Context, Document] = Kleisli { ctx =>
    taskFromUnsafe {
      val doc = new PDFDocument()
      doc.load(pdf.file)
      doc
    }
  }

  private[this] def renderImages(r: SimpleRenderer, d: Document): Kleisli[Task, Context, List[RenderedImage]] = Kleisli { ctx =>
    taskFromUnsafe {
      r.render(d).toList.map(_.asInstanceOf[RenderedImage])
    }
  }

  private[this] def makeRenderer(): Kleisli[Task, Context, SimpleRenderer] = Kleisli { ctx =>
    taskFromUnsafe {
      val renderer: SimpleRenderer = new SimpleRenderer()
      renderer.setResolution(300)
      renderer
    }
  }

  private[this] def saveImage(i: RenderedImage, f: File): Kleisli[Task, Context, SavedPageImage] = Kleisli { ctx =>
    taskFromUnsafe[SavedPageImage] {
      val success = ImageIO.write(i, "jpg", f)

      if (success) {
        ctx.logger.info(s"Wrote image ${f.getAbsolutePath}")
        SavedPageImage(f)
      } else {
        throw CouldNotWriteFile(f)
      }
    }
  }

  private[this] def saveImages(renderedImages: List[RenderedImage], pdf: SavedPdf): Kleisli[Task, Context, List[SavedPageImage]] = Kleisli { ctx =>
    val reversedWithIndex = renderedImages.reverse.zipWithIndex
    val imageTasks = reversedWithIndex.map { case (ri, i) =>
      val file = new File(pdf.file.getParent, s"page-${i+1}.jpg")
      saveImage(ri, file)
    }.map(_.run(ctx))
      Task.gatherUnordered(imageTasks)
  }

  private[this] def delete(file: File): Task[DeletedFile] = taskFromUnsafe {
    if (file.isDirectory)
      Option(file.listFiles).map(_.toList).getOrElse(Nil).foreach(delete)
    if(file.delete) DeletedFile(file) else throw new CouldNotDelete(file)
  }

  object FailedToCastImage extends NoStackTrace
  object NoFileArg extends NoStackTrace
  case class CouldNotWriteFile(f: File) extends NoStackTrace
  case class CouldNotDelete(f: File) extends NoStackTrace
}
