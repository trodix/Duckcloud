package com.trodix.documentstorage.entity;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "node")
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long dbId;

    private String uuid;

    private String bucket;

    private String directoryPath;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "node_aspect")
    private List<Aspect> aspects;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "node_property")
    private List<Property> properties;

}
