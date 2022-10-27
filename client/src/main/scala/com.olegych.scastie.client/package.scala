package com.olegych.scastie

import io.circe.syntax._
import io.circe._

import org.scalajs.dom.window

package object client {

  def dontSerialize[T](fallback: T): Codec[T] = new Codec[T] {
    override def apply(subtype: T): Json = Json.Null
    override def apply(c: HCursor): Decoder.Result[T] = Right(fallback)
  }

  def dontSerializeOption[T]: Codec[T] = new Codec[T] {
    override def apply(subtype: T): Json = Json.Null
    override def apply(c: HCursor): Decoder.Result[T] = Right(null.asInstanceOf[T])
  }

  def dontSerializeList[T]: Codec[List[T]] =
    dontSerialize(List())

  val isMac: Boolean = window.navigator.userAgent.contains("Mac")
  val isMobile: Boolean = "Android|webOS|Mobi|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Samsung".r.unanchored
    .matches(window.navigator.userAgent)
}
