package workflow.utils

import java.io.{File, PrintWriter}

import scala.io.Source

object Io {
  def writeFile(content: String, file: File): Unit = {
    val writer = new PrintWriter(file)
    writer.println(content)
    writer.close()
  }

  def writeLinesFile(lines: Iterator[String], file: File): Unit = {
    val writer = new PrintWriter(file)
    lines.foreach(writer.println)
    writer.close()
  }

  def readFile(file: File): String = {
    val reader = Source.fromFile(file)
    val content = reader.getLines().mkString("\n")
    reader.close()
    content
  }

}
