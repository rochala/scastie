package org.scastie.client.laminar.sse

import com.raquo.airstream.core.{EventStream as AirstreamEventStream, Observer}
import org.scalajs.dom
import org.scalajs.dom.{EventSource, WebSocket, Event, MessageEvent, CloseEvent}
import io.circe.Decoder
import io.circe.parser.decode
import scala.util.{Try, Success, Failure}

/**
 * SSE/WebSocket event stream for Laminar integration.
 *
 * Migrated from: org.scastie.client.EventStream (React version)
 *
 * This provides EventSource (with WebSocket fallback) connectivity
 * integrated with Airstream for reactive updates.
 */
abstract class ScastieEventStream[T: Decoder]:
  @volatile private var closing = false

  def onMessage(raw: String): Boolean =
    if !closing then
      decode[T](raw).foreach { msg =>
        val shouldClose = handleMessage(msg)
        if shouldClose then close()
      }
      false
    else
      false

  def onOpen(): Unit = handleOpen()
  def onError(error: String): Unit = handleError(error)
  def onClose(reason: Option[String]): Unit = handleClose(reason)

  def close(force: Boolean = false): Unit =
    closing = true
    if !force then onClose(None)

  // Abstract methods to be implemented by subclasses
  protected def handleMessage(msg: T): Boolean
  protected def handleOpen(): Unit
  protected def handleError(error: String): Unit
  protected def handleClose(reason: Option[String]): Unit

/**
 * EventSource-based stream (Server-Sent Events).
 */
class EventSourceStream[T: Decoder](
  uri: String,
  onMessageHandler: T => Boolean,
  onOpenHandler: () => Unit,
  onErrorHandler: String => Unit,
  onCloseHandler: Option[String] => Unit
) extends ScastieEventStream[T]:

  private def handleOpenEvent(e: Event): Unit = onOpen()

  private def handleMessageEvent(e: MessageEvent): Unit =
    try
      onMessage(e.data.toString)
    catch
      case error: Throwable =>
        error.printStackTrace()
        println(e)
        println(e.data)

  private def handleErrorEvent(e: Event): Unit =
    if e.eventPhase == EventSource.CLOSED then
      eventSource.close()
      onClose(None)
    else
      onError(e.`type`)

  override def close(force: Boolean = false): Unit =
    super.close(force)
    eventSource.close()

  protected def handleMessage(msg: T): Boolean = onMessageHandler(msg)
  protected def handleOpen(): Unit = onOpenHandler()
  protected def handleError(error: String): Unit = onErrorHandler(error)
  protected def handleClose(reason: Option[String]): Unit = onCloseHandler(reason)

  private val eventSource: EventSource = new EventSource(uri)
  eventSource.onopen = handleOpenEvent _
  eventSource.onmessage = handleMessageEvent _
  eventSource.onerror = handleErrorEvent _

/**
 * WebSocket-based stream (fallback for EventSource).
 */
class WebSocketStream[T: Decoder](
  uri: String,
  onMessageHandler: T => Boolean,
  onOpenHandler: () => Unit,
  onErrorHandler: String => Unit,
  onCloseHandler: Option[String] => Unit
) extends ScastieEventStream[T]:

  private def handleOpenEvent(e: Event): Unit = onOpen()

  private def handleMessageEvent(e: MessageEvent): Unit =
    onMessage(e.data.toString)

  private def handleCloseEvent(e: CloseEvent): Unit =
    onClose(Some(e.reason))

  override def close(force: Boolean = false): Unit =
    super.close(force)
    socket.close()

  protected def handleMessage(msg: T): Boolean = onMessageHandler(msg)
  protected def handleOpen(): Unit = onOpenHandler()
  protected def handleError(error: String): Unit = onErrorHandler(error)
  protected def handleClose(reason: Option[String]): Unit = onCloseHandler(reason)

  private val protocol: String =
    if dom.window.location.protocol == "https:" then "wss" else "ws"
  private val fullUri: String = s"$protocol://${dom.window.location.host}$uri"
  private val socket: WebSocket = new WebSocket(uri)

  socket.onopen = handleOpenEvent _
  socket.onclose = handleCloseEvent _
  socket.onmessage = handleMessageEvent _

/**
 * Connection manager for SSE/WebSocket streams.
 */
object ScastieEventStream:

  /**
   * Connect to SSE with WebSocket fallback.
   *
   * @param eventSourceUri URI for EventSource connection
   * @param websocketUri URI for WebSocket connection (fallback)
   * @param onMessage Handler for messages (returns true to close)
   * @param onOpen Handler for connection open
   * @param onError Handler for errors
   * @param onClose Handler for connection close
   * @param onConnectionError Handler for connection failures
   * @return Try[ScastieEventStream[T]] - Success if connected, Failure otherwise
   */
  def connect[T: Decoder](
    eventSourceUri: String,
    websocketUri: String,
    onMessage: T => Boolean,
    onOpen: () => Unit = () => (),
    onError: String => Unit = _ => (),
    onClose: Option[String] => Unit = _ => (),
    onConnectionError: String => Unit = _ => ()
  ): Try[ScastieEventStream[T]] =
    // Try EventSource first
    Try(new EventSourceStream(eventSourceUri, onMessage, onOpen, onError, onClose))
      .recoverWith { case eventSourceError =>
        // Fallback to WebSocket
        onConnectionError(s"EventSource failed: $eventSourceError")
        Try(new WebSocketStream(websocketUri, onMessage, onOpen, onError, onClose))
          .recoverWith { case websocketError =>
            onConnectionError(s"WebSocket failed: $websocketError")
            Failure(new Exception(s"Both EventSource and WebSocket failed"))
          }
      }

  /**
   * Create an Airstream EventStream from SSE/WebSocket connection.
   *
   * @param eventSourceUri URI for EventSource connection
   * @param websocketUri URI for WebSocket connection (fallback)
   * @return AirstreamEventStream[T] that emits messages from the connection
   */
  def createStream[T: Decoder](
    eventSourceUri: String,
    websocketUri: String
  ): AirstreamEventStream[T] =
    AirstreamEventStream.fromCustomSource[T, ScastieEventStream[T]](
      start = (fireValue, fireError, getStartIndex, getIsStarted) =>
        var streamOpt: Option[ScastieEventStream[T]] = None

        connect(
          eventSourceUri,
          websocketUri,
          onMessage = { msg =>
            fireValue(msg)
            false // Don't close on each message
          },
          onOpen = () => (),
          onError = error => fireError(new Exception(error)),
          onClose = reason => (),
          onConnectionError = error => fireError(new Exception(error))
        ) match
          case Success(stream) =>
            streamOpt = Some(stream)
            stream
          case Failure(error) =>
            fireError(error)
            null.asInstanceOf[ScastieEventStream[T]]
      ,
      stop = stream =>
        if stream != null then stream.close()
    )
