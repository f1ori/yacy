package net.yacy.cider;

import java.io.BufferedWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

public class AugmentedHtmlStream extends FilterOutputStream {
	private Writer out;

	public AugmentedHtmlStream(OutputStream out, Charset charset) {
		super(out);
		this.out = new BufferedWriter(new OutputStreamWriter(out, charset));
	}
	
	public void write(int b) throws IOException {
		this.out.write(b);
	}

}
