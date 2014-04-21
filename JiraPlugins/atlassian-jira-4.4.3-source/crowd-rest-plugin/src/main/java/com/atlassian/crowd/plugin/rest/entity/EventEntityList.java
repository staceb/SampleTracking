package com.atlassian.crowd.plugin.rest.entity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name="events")
@XmlAccessorType(XmlAccessType.FIELD)
public class EventEntityList
{
    @XmlAttribute(name="newEventToken")
    private final String newEventToken;

    @XmlElements({
            @XmlElement(name="userEvent", type=UserEventEntity.class),
            @XmlElement(name="groupEvent", type=GroupEventEntity.class),
            @XmlElement(name="userMembershipEvent", type=UserMembershipEventEntity.class),
            @XmlElement(name="groupMembershipEvent", type=GroupMembershipEventEntity.class)
    })
    private List<AbstractEventEntity> events;

    /**
     * JAXB requires a no-arg constructor
     */
    private EventEntityList()
    {
        this.newEventToken = null;
    }

    public EventEntityList(String newEventToken, List<AbstractEventEntity> events)
    {
        this.newEventToken = newEventToken;
        this.events = events;
    }
}
