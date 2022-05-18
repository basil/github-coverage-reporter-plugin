package io.jenkins.plugins.gcr.parsers;

import hudson.FilePath;
import io.jenkins.plugins.gcr.models.CoberturaCoverage;
import io.jenkins.plugins.gcr.models.Coverage;
import io.jenkins.plugins.gcr.utils.XmlUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;

public class CoberturaParser implements CoverageParser {

    public CoberturaParser() {

    }


    @Override
    public Coverage parse(FilePath filepath) throws ParserException {
        JAXBContext jaxbContext;
        Thread t = Thread.currentThread();
        ClassLoader orig = t.getContextClassLoader();
        t.setContextClassLoader(CoberturaParser.class.getClassLoader());
        try {
            jaxbContext = JAXBContext.newInstance(CoberturaCoverage.class);
        } catch (JAXBException e) {
            throw new ParserException("Failed to initialize JAXB context", e);
        } finally {
            t.setContextClassLoader(orig);
        }
        try {
            SAXSource source = XmlUtils.getSAXSource(filepath);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Coverage coverage = (Coverage) jaxbUnmarshaller.unmarshal(source);

            return coverage;
        } catch (Exception ex) {
            String message = String.format("Failed to parse Cobertura coverage for filepath '%s'", filepath);
            throw new ParserException(message, ex);
        }
    }

}
