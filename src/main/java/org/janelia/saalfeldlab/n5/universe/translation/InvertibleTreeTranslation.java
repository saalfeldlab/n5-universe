package org.janelia.saalfeldlab.n5.universe.translation;

import com.google.gson.Gson;
import java.util.Map;
import org.janelia.saalfeldlab.n5.universe.container.ContainerMetadataNode;

public class InvertibleTreeTranslation extends TreeTranslation {

	protected JqContainerTranslation invFun;

	public InvertibleTreeTranslation( 
			final ContainerMetadataNode root,
			final Gson gson,
			final String fwd, final String inv) {
		super( root, gson, fwd );
		invFun = new JqContainerTranslation( inv, gson );
	}

	public JqContainerTranslation getInverseTranslationFunction() {
		return invFun;
	}
	
	public void updateOriginal() {
		rootOrig = invFun.apply(rootTranslated);
		rootOrig.addPathsRecursive();
	}

	public <T> void setTranslatedAttribute(String pathName, String key, T attribute) {
		rootTranslated.setAttribute(pathName, key, attribute);
		updateOriginal();
	}
	
	public <T> void setTranslatedAttributes(String pathName, Map<String, ?> attributes) {
		rootTranslated.setAttributes(pathName, attributes);
		updateOriginal();
	}

}
