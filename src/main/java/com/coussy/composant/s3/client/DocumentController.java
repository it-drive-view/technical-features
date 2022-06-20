package com.coussy.composant.s3.client;

import java.io.IOException;
import java.time.LocalDate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@RestController
public class DocumentController {

	private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

	@Value("${epr.document.metrics.prefix:}")
	private String metricsPrefix;

	@Resource
	private DocumentService documentService;

	@PostConstruct
	public void init() {
		metricsPrefix = StringUtils.trimToEmpty(metricsPrefix);
		if (StringUtils.isNotBlank(metricsPrefix) && !StringUtils.endsWith(metricsPrefix, ".")) {
			metricsPrefix += ".";
		}
	}

	// Specific management of IllegalArgumentException (Not in
	// RestResponseEntityExceptionHandler)
	@ExceptionHandler(IllegalArgumentException.class)
	public void handleIllegalArgument(final IllegalArgumentException exception, final NativeWebRequest request)
			throws IOException {
		HttpServletResponse response = request.getNativeResponse(HttpServletResponse.class);
		if (response != null) {
			response.sendError(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
		}
	}

	@GetMapping(path = "/v1/download/{insurer}/profile/kid/{productId}/{profileId}/lang/{lang}")
	public void downloadDocument(@PathVariable final String insurer, @PathVariable final String productId,
			@PathVariable final String profileId, @PathVariable final String lang,
			@RequestParam(required = false) final String key, @ApiIgnore final NativeWebRequest request)
			throws IOException {
		AbstractDocument current = null;
		String nature = "ACTION";
		documentService.stream(nature, current, request, "AXA-INSURANCE", "product-name");
	}

	@PostMapping("/documents")
	@ApiOperation(value = "Store provided documents.")
	public ResponseEntity<String> uploadDocument(@RequestPart("file") final MultipartFile file,
			@RequestParam(name = "language") final String language,
			@RequestParam(name = "parentid") final String parentid, @RequestParam("id") final String id,
			@RequestParam(name = "effectiveDate") final LocalDate effectiveDate) {

		try {
			documentService.upload(file, id, parentid, "AXA-producer", language, effectiveDate);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ok");
	}

}
