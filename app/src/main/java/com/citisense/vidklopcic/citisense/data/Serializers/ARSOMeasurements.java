package com.citisense.vidklopcic.citisense.data.Serializers;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name="arsopodatki", strict=false)
public class ARSOMeasurements {
    @Element(name="vir")
    public String source;

    @Element(name="predlagan_zajem")
    public String suggested_measurement;

    @Element(name="predlagan_zajem_perioda")
    public String suggested_interval;

    @Element(name="datum_priprave")
    public String time;

    @ElementList(inline=true)
    public List<ARSOStation> stations;
}
