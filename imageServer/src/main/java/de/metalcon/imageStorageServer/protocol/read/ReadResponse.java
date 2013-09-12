package de.metalcon.imageStorageServer.protocol.read;

import de.metalcon.imageStorageServer.protocol.ProtocolConstants;
import de.metalcon.imageStorageServer.protocol.Response;

public class ReadResponse extends Response {

	// TODO: write helpful solution messages for missing parameters!
	// TODO: JavaDoc
	// TODO: refactor code using constants (in malformed-error methods)
	// TODO: refactor to match the other protocol request/response patterns

	public void addNoImageIdentifierError() {
		this.parameterMissing(
				ProtocolConstants.Parameters.Read.IMAGE_IDENTIFIER,
				"The image identifier is missing. Please deliver one");

	}

	public void addOriginalImageFlagMalformedError() {
		this.addStatusMessage(
				"request corrupt: parameter \"originalFlag\" is malformed",
				"The originalImage flag String is malformed");

	}

	public void addNoOriginalFlagError() {
		this.parameterMissing(ProtocolConstants.Parameters.Read.ORIGINAL_FLAG,
				"The originalImage flag is missing. Please deliver one");

	}

	public void addNoImageWidthError() {
		this.parameterMissing(
				ProtocolConstants.Parameters.Read.IMAGE_WIDTH,
				"The image width is not given. Either deliver width AND height or leave them out and ask for the unscaled image");
	}

	public void addNoImageHeightError() {
		this.parameterMissing(
				ProtocolConstants.Parameters.Read.IMAGE_HEIGHT,
				"The image height is not given. Either deliver width AND height or leave them out and ask for the unscaled image");
	}

	public void addImageWidthMalformedError() {
		this.addStatusMessage(
				"request corrupt: parameter \"imageWidth\" is malformed",
				"The imageWidth flag String is malformed");

	}

	public void addImageHeightMalformedError() {
		this.addStatusMessage(
				"request corrupt: parameter \"imageHeight\" is malformed",
				"The imageHeight flag String is malformed");

	}

	public void addImageNotFoundError() {
		this.addStatusMessage(
				ProtocolConstants.StatusMessage.Read.NO_IMAGE_FOUND, "");
	}

	public void addGeometryBiggerThanOriginalWarning() {
		this.addStatusMessage(
				ProtocolConstants.StatusMessage.Read.GEOMETRY_BIGGER_THAN_ORIGINAL,
				"");
	}
}