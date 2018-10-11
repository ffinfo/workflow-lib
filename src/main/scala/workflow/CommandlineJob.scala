package workflow

import java.io.File

import nl.biopet.utils.process.Sys

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait CommandlineJob extends Job {
  def cmd: String

  def run(): Future[_] = {
    //TODO: backends
    logger.info(s"cmd: $cmd")
    Sys.execAsyncString(cmd).get

  }
}

object CommandlineJob {
  private def template(cmd: String, cwd: File, stdout: File, stderr: File): String =
    s"""
      |#!/bin/bash
      |set -e -p pipefail
      |
      |mkdir -p $cwd
      |cd $cwd
      |
      |( $cmd ) > $stdout 2> $stderr
      |RC=$$?
      |
    """.stripMargin
}