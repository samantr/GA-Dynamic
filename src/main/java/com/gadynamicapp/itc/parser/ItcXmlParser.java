package com.gadynamicapp.itc.parser;

import com.gadynamicapp.itc.model.ClassRoomOption;
import com.gadynamicapp.itc.model.ClassTimeOption;
import com.gadynamicapp.itc.model.Distribution;
import com.gadynamicapp.itc.model.DistributionClassRef;
import com.gadynamicapp.itc.model.ItcClass;
import com.gadynamicapp.itc.model.ItcConfig;
import com.gadynamicapp.itc.model.ItcRoom;
import com.gadynamicapp.itc.model.OptimizationWeights;
import com.gadynamicapp.itc.model.RoomUnavailable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ItcXmlParser {
    public ParseResult parse(Path input) throws IOException, ItcParseException {
        Document document = parseDocument(input);
        Element root = document.getDocumentElement();
        if (root == null || !"problem".equals(root.getTagName())) {
            throw new ItcParseException("Expected root element <problem>.");
        }

        List<ItcRoom> rooms = parseRooms(firstDirectChild(root, "rooms"));
        CourseParseResult courses = parseCourses(firstDirectChild(root, "courses"));
        List<Distribution> distributions = parseDistributions(firstDirectChild(root, "distributions"));
        int studentCount = directChildren(firstDirectChild(root, "students"), "student").size();

        ItcConfig config = new ItcConfig(
                attribute(root, "name"),
                intAttributeOrDefault(root, "nrDays", 0),
                intAttributeOrDefault(root, "nrWeeks", 0),
                intAttributeOrDefault(root, "slotsPerDay", 0),
                parseOptimizationWeights(firstDirectChild(root, "optimization")),
                courses.courseCount(),
                courses.configCount(),
                courses.subpartCount(),
                studentCount
        );

        return new ParseResult(config, rooms, courses.classes(), distributions);
    }

    private Document parseDocument(Path input) throws IOException, ItcParseException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(input.toFile());
        } catch (ParserConfigurationException | SAXException ex) {
            throw new ItcParseException("Invalid XML document.", ex);
        }
    }

    private OptimizationWeights parseOptimizationWeights(Element optimizationElement) throws ItcParseException {
        if (optimizationElement == null) {
            return new OptimizationWeights(0, 0, 0, 0);
        }

        return new OptimizationWeights(
                intAttributeOrDefault(optimizationElement, "time", 0),
                intAttributeOrDefault(optimizationElement, "room", 0),
                intAttributeOrDefault(optimizationElement, "distribution", 0),
                intAttributeOrDefault(optimizationElement, "student", 0)
        );
    }

    private List<ItcRoom> parseRooms(Element roomsElement) throws ItcParseException {
        List<ItcRoom> rooms = new ArrayList<>();
        for (Element roomElement : directChildren(roomsElement, "room")) {
            List<RoomUnavailable> unavailableTimes = new ArrayList<>();
            for (Element unavailableElement : directChildren(roomElement, "unavailable")) {
                unavailableTimes.add(new RoomUnavailable(
                        attribute(unavailableElement, "days"),
                        intAttributeOrDefault(unavailableElement, "start", 0),
                        intAttributeOrDefault(unavailableElement, "length", 0),
                        attribute(unavailableElement, "weeks")
                ));
            }

            rooms.add(new ItcRoom(
                    attribute(roomElement, "id"),
                    intAttributeOrDefault(roomElement, "capacity", 0),
                    unavailableTimes
            ));
        }
        return rooms;
    }

    private CourseParseResult parseCourses(Element coursesElement) throws ItcParseException {
        List<ItcClass> classes = new ArrayList<>();
        int configCount = 0;
        int subpartCount = 0;
        List<Element> courseElements = directChildren(coursesElement, "course");

        for (Element courseElement : courseElements) {
            String courseId = attribute(courseElement, "id");
            List<Element> configElements = directChildren(courseElement, "config");
            configCount += configElements.size();

            for (Element configElement : configElements) {
                String configId = attribute(configElement, "id");
                List<Element> subpartElements = directChildren(configElement, "subpart");
                subpartCount += subpartElements.size();

                for (Element subpartElement : subpartElements) {
                    String subpartId = attribute(subpartElement, "id");
                    for (Element classElement : directChildren(subpartElement, "class")) {
                        classes.add(parseClass(classElement, courseId, configId, subpartId));
                    }
                }
            }
        }

        return new CourseParseResult(courseElements.size(), configCount, subpartCount, classes);
    }

    private ItcClass parseClass(
            Element classElement,
            String courseId,
            String configId,
            String subpartId
    ) throws ItcParseException {
        List<ClassRoomOption> rooms = new ArrayList<>();
        for (Element roomElement : directChildren(classElement, "room")) {
            rooms.add(new ClassRoomOption(
                    attribute(roomElement, "id"),
                    intAttributeOrDefault(roomElement, "penalty", 0)
            ));
        }

        List<ClassTimeOption> times = new ArrayList<>();
        for (Element timeElement : directChildren(classElement, "time")) {
            times.add(new ClassTimeOption(
                    attribute(timeElement, "days"),
                    intAttributeOrDefault(timeElement, "start", 0),
                    intAttributeOrDefault(timeElement, "length", 0),
                    attribute(timeElement, "weeks"),
                    intAttributeOrDefault(timeElement, "penalty", 0)
            ));
        }

        return new ItcClass(
                attribute(classElement, "id"),
                courseId,
                configId,
                subpartId,
                intAttributeOrDefault(classElement, "limit", 0),
                optionalAttribute(classElement, "parent"),
                booleanAttributeOrNull(classElement, "roomRequired"),
                rooms,
                times
        );
    }

    private List<Distribution> parseDistributions(Element distributionsElement) throws ItcParseException {
        List<Distribution> distributions = new ArrayList<>();
        for (Element distributionElement : directChildren(distributionsElement, "distribution")) {
            List<DistributionClassRef> classRefs = new ArrayList<>();
            for (Element classElement : directChildren(distributionElement, "class")) {
                classRefs.add(new DistributionClassRef(attribute(classElement, "id")));
            }

            distributions.add(new Distribution(
                    attribute(distributionElement, "type"),
                    booleanAttributeOrDefault(distributionElement, "required", false),
                    integerAttributeOrNull(distributionElement, "penalty"),
                    classRefs
            ));
        }
        return distributions;
    }

    private String attribute(Element element, String name) {
        if (element == null) {
            return "";
        }
        return element.getAttribute(name);
    }

    private String optionalAttribute(Element element, String name) {
        String value = attribute(element, name);
        return value.isBlank() ? null : value;
    }

    private int intAttributeOrDefault(Element element, String name, int defaultValue) throws ItcParseException {
        String value = attribute(element, name);
        if (value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new ItcParseException("Attribute '" + name + "' must be an integer, got '" + value + "'.", ex);
        }
    }

    private Integer integerAttributeOrNull(Element element, String name) throws ItcParseException {
        String value = attribute(element, name);
        if (value.isBlank()) {
            return null;
        }
        return intAttributeOrDefault(element, name, 0);
    }

    private boolean booleanAttributeOrDefault(Element element, String name, boolean defaultValue) {
        String value = attribute(element, name);
        if (value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private Boolean booleanAttributeOrNull(Element element, String name) {
        String value = attribute(element, name);
        if (value.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    private Element firstDirectChild(Element parent, String tagName) {
        List<Element> children = directChildren(parent, tagName);
        return children.isEmpty() ? null : children.get(0);
    }

    private List<Element> directChildren(Element parent, String tagName) {
        List<Element> children = new ArrayList<>();
        if (parent == null) {
            return children;
        }

        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element childElement && tagName.equals(childElement.getTagName())) {
                children.add(childElement);
            }
        }
        return children;
    }

    private record CourseParseResult(
            int courseCount,
            int configCount,
            int subpartCount,
            List<ItcClass> classes
    ) {
        private CourseParseResult {
            classes = List.copyOf(classes);
        }
    }
}