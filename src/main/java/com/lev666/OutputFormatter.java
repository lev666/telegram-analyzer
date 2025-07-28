package com.lev666;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface OutputFormatter {
    void write(List<Message> messages, File directory) throws IOException;
}
