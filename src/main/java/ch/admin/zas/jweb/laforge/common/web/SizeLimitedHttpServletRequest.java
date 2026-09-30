package ch.admin.zas.jweb.laforge.common.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * Enveloppe une requête pour interrompre la lecture du corps dès qu'elle dépasse
 * {@code maxBytes}, y compris lorsque {@code Content-Length} est absent ou mensonger (transfert
 * fragmenté). Protège la mémoire du serveur indépendamment de l'en-tête déclaré par le client
 * (voir {@link MaxRequestBodySizeFilter} pour le rejet rapide basé sur {@code Content-Length}).
 */
final class SizeLimitedHttpServletRequest extends HttpServletRequestWrapper {

    private final long maxBytes;

    SizeLimitedHttpServletRequest(HttpServletRequest request, long maxBytes) {
        super(request);
        this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        var delegate = super.getInputStream();
        return new ServletInputStream() {

            private long readBytes;

            @Override
            public int read() throws IOException {
                var value = delegate.read();
                if (value >= 0) {
                    checkLimit(1);
                }
                return value;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                var read = delegate.read(b, off, len);
                if (read > 0) {
                    checkLimit(read);
                }
                return read;
            }

            private void checkLimit(int increment) throws IOException {
                readBytes += increment;
                if (readBytes > maxBytes) {
                    throw new IOException(
                            "Le corps de la requête dépasse la taille maximale autorisée de " + maxBytes + " octets.");
                }
            }

            @Override
            public boolean isFinished() {
                return delegate.isFinished();
            }

            @Override
            public boolean isReady() {
                return delegate.isReady();
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                delegate.setReadListener(readListener);
            }
        };
    }
}
