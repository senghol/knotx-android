package com.senghol.knotx.utils.enums

enum class CallState {
    Idle,
    Calling,
    Ringing,
    Connecting,
    InCall,
    Ended,
    BusyOnAnotherCall  // New state for busy call
}
