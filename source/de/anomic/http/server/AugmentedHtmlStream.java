package de.anomic.http.server;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import de.anomic.search.Switchboard;

import net.yacy.kelondro.data.meta.DigestURI;
// import net.yacy.sci.*;

public class AugmentedHtmlStream extends FilterOutputStream {
	private Writer out;
	private ByteArrayOutputStream buffer;
	private Charset charset;
	private DigestURI url;
	private String urlhash;
	private String context;

	public AugmentedHtmlStream(OutputStream out, Charset charset, DigestURI url, String urlhash) {
		super(out);
		this.out = new BufferedWriter(new OutputStreamWriter(out, charset));
		this.buffer = new ByteArrayOutputStream();
		this.charset = charset;
		this.url = url;
		this.urlhash = urlhash;
		this.context = "loadtouser";
	}
	
	public void write(int b) throws IOException {
		this.buffer.write(b);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		this.buffer.write(b, off, len);
	}
	
	public void close() throws IOException {
		StringBuffer b = new StringBuffer(this.buffer.toString(charset.name()));
		b = process(b);
		out.write(b.toString());
		// System.out.println(b);
		out.close();
	}
	
	public StringBuffer process(StringBuffer data) {
		// System.out.println("got something!");
		
		Switchboard sb = Switchboard.getSwitchboard();
		
		if (sb.getConfigBool("proxyAugmentation", false) == true) {
		
			// return AugmentHtmlStream.process (data, charset, url, context);
			
			return data;

		} else {
			return data;
		}

	}
	
	public static boolean supportsMime(String mime) {
		// System.out.println("mime" +mime);
		return mime.split(";")[0].equals("text/html");
	}

}