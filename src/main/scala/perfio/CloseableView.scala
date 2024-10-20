package perfio

private trait CloseableView {
  private[perfio] def markClosed(): Unit
}
