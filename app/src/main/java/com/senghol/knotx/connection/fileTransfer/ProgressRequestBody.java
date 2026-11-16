package com.senghol.knotx.connection.fileTransfer;


import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;



public class ProgressRequestBody extends RequestBody {

    private final RequestBody requestBody;
    private final ProgressListener progressListener;
    public static final int PROGRESS_INTERVAL = 10; // Reporting interval in percentage

    public interface ProgressListener {
        void onProgress(int progressPercentage);
    }

    public ProgressRequestBody(RequestBody requestBody, ProgressListener progressListener) {
        this.requestBody = requestBody;
        this.progressListener = progressListener;
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        long contentLength = contentLength();
        ForwardingSink forwardingSink = new ForwardingSink(sink) {
            long bytesWritten = 0;
            int lastProgress = 0;

            @Override
            public void write(@NonNull okio.Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                bytesWritten += byteCount;

                // Calculate progress percentage
                int progress = (int) ((bytesWritten * 100) / contentLength);

                // Report progress at 10% intervals
                if (progress - lastProgress >= PROGRESS_INTERVAL || progress == 100) {
                    lastProgress = progress;
                    if (progressListener != null) {
                        progressListener.onProgress(progress);
                    }
                }
            }
        };

        BufferedSink bufferedSink = Okio.buffer(forwardingSink);
        requestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }
}
