package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.config.SsoProperties;
import com.lzlz.springboot.security.dto.SsoUserInfo;
import com.lzlz.springboot.security.exception.SsoAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SsoClientService {

    private final SsoProperties ssoProperties;
    private final RestTemplate restTemplate;

    public SsoClientService(SsoProperties ssoProperties, RestTemplate restTemplate) {
        this.ssoProperties = ssoProperties;
        this.restTemplate = restTemplate;
    }

    public String buildLoginUrl() {
        ensureEnabled();
        return UriComponentsBuilder.fromHttpUrl(ssoProperties.getLoginUrl())
                .queryParam("service", ssoProperties.getServiceUrl())
                .build()
                .encode()
                .toUriString();
    }

    public SsoUserInfo validateTicket(String ticket) {
        ensureEnabled();
        if (ticket == null || ticket.isBlank()) {
            throw new SsoAuthenticationException("SSO ticket is required");
        }

        String validateUrl = UriComponentsBuilder.fromHttpUrl(ssoProperties.getValidateUrl())
                .queryParam("service", ssoProperties.getServiceUrl())
                .queryParam("ticket", ticket)
                .build()
                .encode()
                .toUriString();

        String response;
        try {
            response = restTemplate.getForObject(validateUrl, String.class);
        } catch (RestClientException e) {
            throw new SsoAuthenticationException("Failed to validate SSO ticket", e);
        }

        return parseServiceValidateResponse(response);
    }

    private void ensureEnabled() {
        if (!ssoProperties.isEnabled()) {
            throw new SsoAuthenticationException("SSO login is disabled");
        }
    }

    private SsoUserInfo parseServiceValidateResponse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new SsoAuthenticationException("Empty SSO validation response");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            disableExternalEntity(factory);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            Element failure = firstElement(document, "authenticationFailure");
            if (failure != null) {
                String code = failure.getAttribute("code");
                String message = failure.getTextContent() == null ? "" : failure.getTextContent().trim();
                throw new SsoAuthenticationException("SSO validation failed: " + code + " " + message);
            }

            Element success = firstElement(document, "authenticationSuccess");
            if (success == null) {
                throw new SsoAuthenticationException("SSO validation response missing authenticationSuccess");
            }

            Map<String, String> attributes = childElementValues(success);
            SsoUserInfo userInfo = new SsoUserInfo();
            userInfo.setUsername(attributes.get("user"));
            userInfo.setName(attributes.get("name"));
            userInfo.setEmployeeNumber(attributes.get("employeeNumber"));
            userInfo.setAttributes(attributes);

            if (userInfo.getLocalUsername() == null || userInfo.getLocalUsername().isBlank()) {
                throw new SsoAuthenticationException("SSO validation response missing user identifier");
            }
            return userInfo;
        } catch (SsoAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new SsoAuthenticationException("Failed to parse SSO validation response", e);
        }
    }

    private Element firstElement(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            nodes = document.getElementsByTagName(localName);
        }
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private Map<String, String> childElementValues(Element parent) {
        Map<String, String> values = new LinkedHashMap<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element) {
                String name = element.getLocalName() == null ? element.getNodeName() : element.getLocalName();
                values.put(name, element.getTextContent() == null ? "" : element.getTextContent().trim());
            }
        }
        return values;
    }

    private void disableExternalEntity(DocumentBuilderFactory factory) {
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
        } catch (Exception ignored) {
            // Some XML parsers do not support all hardening flags.
        }
    }
}
