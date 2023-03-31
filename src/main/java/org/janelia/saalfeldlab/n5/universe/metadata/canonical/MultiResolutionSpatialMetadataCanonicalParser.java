package org.janelia.saalfeldlab.n5.universe.metadata.canonical;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Optional;

/**
 * Interface for metadata describing how spatial data are oriented in physical space.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class MultiResolutionSpatialMetadataCanonicalParser extends AbstractMetadataTemplateParser<SpatialMetadataCanonical>{

	public MultiResolutionSpatialMetadataCanonicalParser( final Gson gson, final String translation )
	{
		super( gson, translation );
	}

	@Override
	public Optional<SpatialMetadataCanonical> parseFromMap( final Gson gson, final HashMap<String, JsonElement> attributeMap ) {

		try { 

			return Optional.empty();
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}

		return Optional.empty();
	}

}
