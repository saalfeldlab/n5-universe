package org.janelia.saalfeldlab.n5.universe.demo;

import org.scijava.Context;
import org.scijava.ui.UIService;

/**
 * Shows the ImageJ main window
 *
 * @author Stefan Hahmann
 */
public class StartImageJ
{

	public static void main( final String... args )
	{
		@SuppressWarnings( "resource" )
		final Context context = new Context();
		final UIService uiService = context.service( UIService.class );
		uiService.showUI();
	}
}
