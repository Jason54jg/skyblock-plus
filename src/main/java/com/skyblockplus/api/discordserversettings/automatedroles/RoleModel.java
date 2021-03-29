package com.skyblockplus.api.discordserversettings.automatedroles;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Embeddable
@Transactional
public class RoleModel {

    private String enable = "false";

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<RoleObject> levels = new ArrayList<>();

    private String stackable = "false";

    public RoleModel() {
    }
}