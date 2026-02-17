package com.example.aegis_be.domain.medical.client;

import com.example.aegis_be.domain.medical.client.dto.BedAvailabilityItem;
import com.example.aegis_be.domain.medical.client.dto.HospitalLocationItem;
import com.example.aegis_be.global.error.BusinessException;
import com.example.aegis_be.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EmergencyApiClient {

    private final EmergencyApiProperties properties;
    private final HttpClient httpClient;

    public EmergencyApiClient(EmergencyApiProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public List<HospitalLocationItem> getHospitalList(String stage1, String stage2) {
        String url = properties.getBaseUrl() + "/getEgytListInfoInqire"
                + "?serviceKey=" + encode(properties.getServiceKey())
                + "&Q0=" + encode(stage1)
                + (stage2 != null && !stage2.isBlank() ? "&Q1=" + encode(stage2) : "")
                + "&numOfRows=" + properties.getMaxResults()
                + "&pageNo=1";

        String xml = callApi(url, "hospital-list");
        return parseHospitalList(xml);
    }

    public List<BedAvailabilityItem> getBedAvailability(String stage1, String stage2) {
        String url = properties.getBaseUrl() + "/getEmrrmRltmUsefulSckbdInfoInqire"
                + "?serviceKey=" + encode(properties.getServiceKey())
                + "&STAGE1=" + encode(stage1)
                + (stage2 != null && !stage2.isBlank() ? "&STAGE2=" + encode(stage2) : "")
                + "&numOfRows=" + properties.getMaxResults()
                + "&pageNo=1";

        String xml = callApi(url, "bed-availability");
        return parseBedAvailability(xml);
    }

    public String fetchHospitalDepartments(String hpid) {
        String url = properties.getBaseUrl() + "/getEgytBassInfoInqire"
                + "?serviceKey=" + encode(properties.getServiceKey())
                + "&HPID=" + hpid
                + "&numOfRows=1&pageNo=1";

        String xml = callApi(url, "hospital-departments");
        List<Element> items = getItemElements(xml);
        if (items.isEmpty()) {
            return null;
        }
        return getText(items.get(0), "dgidIdName");
    }

    private String callApi(String url, String apiName) {
        try {
            log.debug("Calling {} API: {}", apiName, url.replaceAll("serviceKey=[^&]+", "serviceKey=***"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/xml")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.error("{} API returned HTTP {}", apiName, response.statusCode());
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
            }

            String body = response.body();

            // data.go.kr returns HTTP 200 with error XML on auth/param failures
            String resultCode = extractResultCode(body);
            if (resultCode != null && !"00".equals(resultCode)) {
                String resultMsg = extractResultMsg(body);
                log.error("{} API returned error - resultCode: {}, resultMsg: {}", apiName, resultCode, resultMsg);
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
            }

            return body;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call {} API: {}", apiName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    private String extractResultCode(String xml) {
        int start = xml.indexOf("<resultCode>");
        int end = xml.indexOf("</resultCode>");
        if (start >= 0 && end > start) {
            return xml.substring(start + "<resultCode>".length(), end).trim();
        }
        return null;
    }

    private String extractResultMsg(String xml) {
        int start = xml.indexOf("<resultMsg>");
        int end = xml.indexOf("</resultMsg>");
        if (start >= 0 && end > start) {
            return xml.substring(start + "<resultMsg>".length(), end).trim();
        }
        return null;
    }

    private List<HospitalLocationItem> parseHospitalList(String xml) {
        List<Element> items = getItemElements(xml);
        List<HospitalLocationItem> result = new ArrayList<>();

        for (Element item : items) {
            result.add(HospitalLocationItem.builder()
                    .hpid(getText(item, "hpid"))
                    .dutyName(getText(item, "dutyName"))
                    .dutyAddr(getText(item, "dutyAddr"))
                    .dutyTel1(getText(item, "dutyTel1"))
                    .dutyTel3(getText(item, "dutyTel3"))
                    .dgidIdName(getText(item, "dutyEmclsName"))
                    .dutyInf(getText(item, "dutyInf"))
                    .dutyEryn(getText(item, "dutyEryn"))
                    .wgs84Lat(getDouble(item, "wgs84Lat"))
                    .wgs84Lon(getDouble(item, "wgs84Lon"))
                    .build());
        }

        return result;
    }

    private List<BedAvailabilityItem> parseBedAvailability(String xml) {
        List<Element> items = getItemElements(xml);
        List<BedAvailabilityItem> result = new ArrayList<>();

        for (Element item : items) {
            result.add(BedAvailabilityItem.builder()
                    .hpid(getText(item, "hpid"))
                    .dutyName(getText(item, "dutyName"))
                    .hvec(getInt(item, "hvec"))
                    .hvoc(getInt(item, "hvoc"))
                    .hvcc(getInt(item, "hvcc"))
                    .hvncc(getInt(item, "hvncc"))
                    .hvicc(getInt(item, "hvicc"))
                    .hvgc(getInt(item, "hvgc"))
                    .hvamyn(getText(item, "hvamyn"))
                    .hv1(getText(item, "hv1"))
                    .build());
        }

        return result;
    }

    private List<Element> getItemElements(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            NodeList nodeList = doc.getElementsByTagName("item");
            List<Element> elements = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                elements.add((Element) nodeList.item(i));
            }
            return elements;
        } catch (Exception e) {
            log.error("Failed to parse XML response: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.API_RESPONSE_PARSE_ERROR);
        }
    }

    private String getText(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return null;
        }
        return nodeList.item(0).getTextContent();
    }

    private int getInt(Element element, String tagName) {
        String text = getText(element, tagName);
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double getDouble(Element element, String tagName) {
        String text = getText(element, tagName);
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
