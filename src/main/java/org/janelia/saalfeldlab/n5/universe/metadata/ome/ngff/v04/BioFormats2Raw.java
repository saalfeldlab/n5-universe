package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04;

import java.util.Optional;

import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.readdata.ReadData;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

public class BioFormats2Raw {

	public static final String RELATIVE_PATH = "OME/METADATA.ome.xml";

	public static Optional<BioFormats2Raw> parseMetadata(N5Reader n5, String path) {

		if (!(n5 instanceof N5KeyValueReader))
			return Optional.empty();

		final KeyValueAccess kva = ((N5KeyValueReader)n5).getKeyValueAccess();

		try {

			final ReadData rd = kva.createReadData(
					kva.compose(n5.getURI(), path, RELATIVE_PATH))
					.materialize();
			final SAXBuilder sax = new SAXBuilder();
			final Document doc = sax.build(rd.inputStream());
			return parse(doc.getRootElement());

		} catch (Exception e) {
			e.printStackTrace();
			return Optional.empty();
		}

	}

	public static Optional<BioFormats2Raw> parse(Element xmlElement) {

		// TODO parse the XML
		return Optional.of(new BioFormats2Raw());
	}

	public static void main(String[] args) {

		// tmp for testing
		N5Reader n5r = new N5Factory().openReader("src/test/resources/bf2raw.zarr");
		Optional<BioFormats2Raw> res = BioFormats2Raw.parseMetadata(n5r, "");
		System.out.println(res);
	}

}
