package com.vidklopcic.airsense.data.Serializers;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Created by vidklopcic on 13/01/2017.
 */

@Root(name="postaja", strict=false)
public class ARSOStation {
    @Attribute(name="sifra")
    public String id;

    @Attribute(name="ge_sirina")
    public Double lat;

    @Attribute(name="ge_dolzina")
    public Double lng;

    @Attribute(name="nadm_visina")
    public Double altitude;

    @Element(name="merilno_mesto")
    public String place;

    @Element(name="datum_od")
    public String start_time;

    @Element(name="datum_do")
    public String end_time;

    @Element(required=false)
    public Double so2;

    @Element(required=false)
    public Double co;

    @Element(required=false)
    public Double o3;

    @Element(required=false)
    public Double no2;

    @Element(required=false)
    public Double pm10;


}
