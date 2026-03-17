package laait.httputil

class IncreasingDelay(private var millis: Long) {

    fun sleep() {
        Thread.sleep(millis)
        millis *= 2
    }
}