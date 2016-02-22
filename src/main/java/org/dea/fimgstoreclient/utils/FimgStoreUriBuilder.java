package org.dea.fimgstoreclient.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.dea.fimgstoreclient.FimgStoreConstants;
import org.dea.fimgstoreclient.beans.ImgType;

public class FimgStoreUriBuilder {
	
	private final static Pattern fileKeyPattern = FimgStoreConstants.getPattern("fileKeyPattern");
	private final static String defaultHost = FimgStoreConstants.getString("host");
	private final static String defaultServerContext = FimgStoreConstants.getString("context");
	private static String getActionPath;
	private static String putActionPath;
	private static String delActionPath;
	private String serverContext;
	private URIBuilder uriBuilder;
	
	/**
	 * Empty Constructor uses HTTPS and hostname, context and paths from fimgstoreclient.properties
	 */
	public FimgStoreUriBuilder(){
		this("https", defaultHost, null, defaultServerContext);
	}
	
	public FimgStoreUriBuilder(final String scheme){
		this(scheme, defaultHost, null, defaultServerContext);
	}
	
	public FimgStoreUriBuilder(final String scheme, final String host, final Integer port, final String context){
		this.serverContext = (context.startsWith("/")) ? context : "/" + context;
		getActionPath = this.serverContext + FimgStoreConstants.GET_ACTION_PATH;
		putActionPath = this.serverContext + FimgStoreConstants.PUT_ACTION_PATH;
		delActionPath = this.serverContext + FimgStoreConstants.DEL_ACTION_PATH;
		this.uriBuilder = new URIBuilder().setScheme(scheme).setHost(host);
		if(port != null && port != 80 && port != 443){
			this.uriBuilder.setPort(port);
		}
	}
	

	public URI getFileUri(final String fileKey) throws IllegalArgumentException {
		return buildURI(fileKey, (NameValuePair[]) null);
	}

	/**
	 * If a specified fileType is not available yet (since the background
	 * convert thread has not finished yet!), a corresponding error message will
	 * be returned! If a specified fileType is not available for the given key,
	 * a filenotfoundexception will be returned!
	 * 
	 * @param imgKey
	 * @param type
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public URI getImgUri(final String imgKey, final ImgType type)
			throws IllegalArgumentException {

		NameValuePair param = new BasicNameValuePair(FimgStoreConstants.FILE_TYPE_PARAM, type.toString());

		return buildURI(imgKey, param);
	}

	/**
	 * scalePerc=percentageOfScaling
	 * 
	 * @param imgKey
	 * @param scalePerc
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public URI getImgPercScaledUri(final String imgKey, final int scalePerc)
			throws IllegalArgumentException {

		// scalePerc = percent of imagesize, e.g. 30 => 30%
		if (scalePerc < 1) {
			throw new IllegalArgumentException("Scale percentage is zero or negative.");
		}

		NameValuePair param = new BasicNameValuePair(FimgStoreConstants.SCALE_PERC_PARAM, "" + scalePerc);
		return buildURI(imgKey, param);
	}

	/**
	 * 
	 * 
	 * @param imgKey
	 * @param xPixels
	 * @param yPixels
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public URI getImgXyScaledUri(final String imgKey, final int xPixels, final int yPixels,
			boolean preserveAspect) throws IllegalArgumentException {
		// scaleXY=pixelsX x pixelsY[!] (the ! means 'do NOT respect aspect
		// ratio',
		// cf GraphicsMagick documentation!)
		String presAspMarker = "";
		if (!preserveAspect) {
			presAspMarker = "!";
		}

		final String scaleXY = xPixels + "x" + yPixels + presAspMarker;

		NameValuePair param = new BasicNameValuePair(FimgStoreConstants.SCALE_X_Y_PARAM, scaleXY);
		return buildURI(imgKey, param);
	}

	/**
	 * 
	 * crop=posX x posY x width x height
	 * 
	 * @param imgKey
	 * @param posX
	 * @param posY
	 * @param width
	 * @param height
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public URI getImgCroppedUri(final String imgKey, final int posX, final int posY,
			final int width, final int height) throws IllegalArgumentException {
		final String X = FimgStoreConstants.MULT_LITERAL;
		final String crop = posX + X + posY + X + width + X + height;
		NameValuePair param = new BasicNameValuePair(FimgStoreConstants.CROP_PARAM, crop);
		return buildURI(imgKey, param);
	}

	/**
	 * - any option string for the GraphicsMagick (or ImageMagick) convert
	 * 
	 * command can be used (cf http://www.graphicsmagick.org/convert.html) by
	 * specifying the parameters convertOpts and convertExt: convertOpts ...
	 * specifies the option string for the convert command convertExt ...
	 * specifies the extension of the output file (without the dot!), default =
	 * jpg
	 * 
	 * Examples: for rotation about 35 degress and conversion to png:
	 * convertOpts=-rotate 35 convertExt=png
	 * http://localhost:8880/imagestore/GetImage
	 * ?id=DWWAGAYXTSHYTZVPLTYJSKBF&convertOpts=-rotate+35&convertExt=png note
	 * that the above url is encoded into UTF-8 format!
	 * 
	 * Use the convenience functions in util.GetImageClient to create valid
	 * retrieval urls!)
	 * 
	 * @param imgKey
	 * @param convertOps
	 * @param convertExt
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public URI getImgConvUri(final String imgKey, final String convertOps,
			final String convertExt) throws IllegalArgumentException {
		NameValuePair ops = new BasicNameValuePair(FimgStoreConstants.CONVERT_OPS_PARAM, convertOps);
		NameValuePair ext = new BasicNameValuePair(FimgStoreConstants.CONVERT_EXT_PARAM, convertExt);

		return buildURI(imgKey, ops, ext);
	}

	/**
	 * Get the metadata for an img
	 * 
	 * @param imgKey
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public URI getImgMdUri(final String imgKey) throws IllegalArgumentException {
		NameValuePair param = new BasicNameValuePair(FimgStoreConstants.FILE_TYPE_PARAM, FimgStoreConstants.MD_FILETYPE_VALUE);
		return buildURI(imgKey, param);
	}

	/*******************
	 * Utility methods *
	 *******************/

	/**
	 * Check the GET parameters and build the fimagestore URL
	 * 
	 * @param fileKey
	 * @param params
	 * @return
	 * @throws IllegalArgumentException
	 */
	private URI buildURI(String fileKey, NameValuePair... params) throws IllegalArgumentException {
		List<NameValuePair> paramsList;
		URI uri = null;

		// validate parameters
		if (fileKey == null) {
			throw new IllegalArgumentException("The fileKey is null.");
		} else if (!fileKeyPattern.matcher(fileKey).matches()) {
			throw new IllegalArgumentException("The fileKey's format is currupt: " + fileKey);
		}

		if (params == null) { // if no params just use the fileKey
			paramsList = new ArrayList<NameValuePair>(1);
		} else if (params.length == 1) {
			paramsList = new ArrayList<NameValuePair>(1);
			paramsList.add(params[0]);
		} else {
			paramsList = Arrays.asList(params);
		}
		paramsList.add(new BasicNameValuePair(FimgStoreConstants.ID_PARAM, fileKey));

		// reset parameters on UriBuilder Object
		uriBuilder.clearParameters();
		// and set the new ones
		uriBuilder.setPath(getActionPath).addParameters(paramsList);

		// build the URI
		try {
			uri = uriBuilder.build();
		} catch (URISyntaxException use) {
			throw new IllegalArgumentException("Fimagestore URL could not be build for filekey "
					+ fileKey + " and parameters " + params.toString());
		}

		return uri;
	}


	public URI getPostUri(){
		URI uri = null; 
		uriBuilder.clearParameters();
		uriBuilder.setPath(putActionPath);
		try{
			uri = uriBuilder.build();
		} catch(URISyntaxException e){
			//getCoffee()
			e.printStackTrace();
		}
		
		return uri;
	}
	
	public URI getDeleteUri(String fileKey){
		URI uri = null;

		// validate parameters
		if (fileKey == null) {
			throw new IllegalArgumentException("The fileKey is null.");
		} else if (!fileKeyPattern.matcher(fileKey).matches()) {
			throw new IllegalArgumentException("The fileKey's format is currupt: " + fileKey);
		}
		
		uriBuilder.clearParameters();
		uriBuilder.setPath(delActionPath).addParameter(FimgStoreConstants.ID_PARAM, fileKey);
		try{
			uri = uriBuilder.build();
		} catch(URISyntaxException e){
			//der Kaffee schmeckt heut irgendwie scheiße
			e.printStackTrace();
		}
		
		return uri;
	}

	public URI getBaseUri() throws URISyntaxException {
		uriBuilder.clearParameters();
		uriBuilder.setPath(serverContext);
		return uriBuilder.build();
	}
}
