package com.trodix.documentstorage.event;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.trodix.documentstorage.persistance.entity.PropertyType;
import com.trodix.documentstorage.persistance.dao.ModelDAO;
import com.trodix.documentstorage.persistance.entity.Model;
import com.trodix.documentstorage.persistance.repository.ModelRepository;
import com.trodix.documentstorage.service.NodeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class AppEvents {

    private final NodeService nodeService;

    private final ModelRepository modelRepository;

    private final ModelDAO modelDAO;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {

        loadData();

    }

    private void loadData() {

        if (modelRepository.count() > 0) {
            log.info("Bootstrap data already loaded");
            return;
        }

        log.info("Loading bootstrap data");

        final List<Model> modelList = new ArrayList<>();

        final Model fruitHeightPropertyModel = new Model();
        fruitHeightPropertyModel.setQname(nodeService.stringToQName("fruit:weight"));
        fruitHeightPropertyModel.setType(PropertyType.DOUBLE);

        modelList.add(fruitHeightPropertyModel);

        final Model fruitNamePropertyModel = new Model();
        fruitNamePropertyModel.setQname(nodeService.stringToQName("fruit:name"));
        fruitNamePropertyModel.setType(PropertyType.STRING);

        modelList.add(fruitNamePropertyModel);

        final Model fruitHavestDatePropertyModel = new Model();
        fruitHavestDatePropertyModel.setQname(nodeService.stringToQName("fruit:harvest-date"));
        fruitHavestDatePropertyModel.setType(PropertyType.DATE);

        modelList.add(fruitHavestDatePropertyModel);

        final Model fruitReferencePropertyModel = new Model();
        fruitReferencePropertyModel.setQname(nodeService.stringToQName("fruit:reference"));
        fruitReferencePropertyModel.setType(PropertyType.LONG);

        modelList.add(fruitReferencePropertyModel);

        modelList.forEach(modelDAO::save);
    }

}
