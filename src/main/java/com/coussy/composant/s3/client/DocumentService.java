package com.coussy.composant.s3.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.coussy.s3.connector.S3Client;

@Service
public class DocumentService {

	private static final String NOT_SUPPORTED = " is not supported by this service";

	private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

	private final ZoneId gmt = ZoneId.of("GMT");

	@Resource
	private S3Client documentsS3client;

	@Value("${epr.documents.base.folders.underlying:underlying}")
	private String underlyingBaseFolder;

	public void upload(final MultipartFile file, final String documentId, final String parentId, final String producer,
			final String language, final LocalDate referenceDate) throws IOException {

		final String storageKey;

		if (file == null || file.getOriginalFilename() == null) {
			logger.error("Failed to upload document, original file name is null");
			return;
		}

		final String originalFilename = file.getOriginalFilename().replaceAll("[^-_a-zA-Z0-9]+", "_");
		storageKey = buildKey(underlyingBaseFolder, parentId, documentId, language, originalFilename);

		final ObjectMetadata meta = documentsS3client.newMetaData(storageKey, file.getSize(), "meta-id", documentId,
				"meta-producer", producer, "meta-language", language);
		String key = documentsS3client.store(storageKey, file.getInputStream(), meta);
	}

	private String buildKey(final Object... paths) {
		StringBuilder path = new StringBuilder();
		for (Object p : paths) {
			String s = Objects.toString(p, "");
			if (StringUtils.isBlank(s)) {
				continue;
			}
			if (path.length() > 0) {
				path.append('/');
			}
			path.append(s);
		}
		return path.toString();
	}

	public void stream(final String nature, final AbstractDocument document, final NativeWebRequest request,
			final String insurer, final String productName) throws IOException {
		if (document == null) {
			throw new IllegalArgumentException("document is mandatory");
		}
		try (S3Object obj = documentsS3client.getContent(document.getStorageKey());) {
			if (obj == null) {
				HttpServletResponse response = request.getNativeResponse(HttpServletResponse.class);
				if (response != null) {
					response.sendError(HttpStatus.NO_CONTENT.value(), "Backend returned empty content");
				}
			} else {
				ObjectMetadata meta = obj.getObjectMetadata();
				long contentLength = meta.getContentLength();
				setResponseHeaders(nature, request, document, meta, obj.getKey(), 0, insurer, productName);
				streamBack(request, contentLength, obj.getObjectContent());
			}
		}
	}

	private void setResponseHeaders(final String nature, final NativeWebRequest request,
			final AbstractDocument document, final ObjectMetadata meta, final String key, final long cacheDuration,
			final String insurer, final String productName) {
		final HttpServletResponse response = request.getNativeResponse(HttpServletResponse.class);
		String fileName = StringUtils.substringAfterLast(key, "/");
		String mimeType = meta.getContentType();
		if ((mimeType == null) || "application/octet-stream".equals(mimeType)) {
			HttpServletRequest req = request.getNativeRequest(HttpServletRequest.class);
			if (req != null) {
				mimeType = req.getServletContext().getMimeType(fileName);
			}
		}
		fileName = productName + "_" + DateTimeFormatter.ofPattern("MM_YYYY").format(document.getReferenceDate())
				+ ".pdf";

		if (mimeType == null) {
			mimeType = "application/octet-stream";
		}

		if (response != null) {
			response.setContentType(mimeType);
			if (request.getParameter("inline") != null) {
				response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");
			} else {
				response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
			}
			if (cacheDuration > 0) {
				response.setHeader("Cache-Control", "public, max-age=" + cacheDuration);
				final String expires = DateTimeFormatter.RFC_1123_DATE_TIME
						.format(ZonedDateTime.ofInstant(Instant.now().plus(cacheDuration, ChronoUnit.SECONDS), gmt));
				response.setHeader("Expires", expires);
			}
			response.setStatus(200);
		}
	}

	private void streamBack(final NativeWebRequest request, final long contentLength, final InputStream in) {
		try {
			final HttpServletResponse response = request.getNativeResponse(HttpServletResponse.class);
			if (response != null) {
				response.setContentLengthLong(contentLength);
				ServletOutputStream out = response.getOutputStream();
				pipeInputToOutput(in, out);
				out.close();
				response.flushBuffer();
			}
		} catch (Exception e) {
			purgeStream(in);
		} finally {
			closeStream(in);
		}
	}

	private void closeStream(final InputStream in) {
		try {
			in.close();
		} catch (Exception e) {
			logger.warn("Failed to close input stream", e);
		}
	}

	private void purgeStream(final InputStream in) {
		try {
			byte[] buff = new byte[512];
			while (in.read(buff, 0, buff.length) != -1) {
				// no-op
			}
		} catch (Exception e) {
			logger.warn("Failed to purge input stream", e);
		}
	}

	private void pipeInputToOutput(final InputStream in, final OutputStream out) throws IOException {
		byte[] buff = new byte[16384];
		int read = -1;
		while ((read = in.read(buff, 0, buff.length)) != -1) {
			out.write(buff, 0, read);
			out.flush();
		}
	}

}
