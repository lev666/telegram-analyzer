package com.lev666;

import java.time.LocalDateTime;

public record Message(String author, LocalDateTime timestamp, String text) { }
