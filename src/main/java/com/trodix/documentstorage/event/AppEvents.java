package com.trodix.documentstorage.event;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.trodix.documentstorage.model.ContentModel;
import com.trodix.documentstorage.persistance.entity.Model;
import com.trodix.documentstorage.persistance.entity.PropertyType;
import com.trodix.documentstorage.service.ModelService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class AppEvents {

    private final ModelService modelService;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {

        loadData();

    }

    private void loadData() {

//        if (modelService.count() > 0) {
//            log.info("Bootstrap data already loaded");
//            return;
//        }

        log.info("Loading bootstrap data");

        final List<Model> modelList = new ArrayList<>();

        final Model typeContent = modelService.buildModel(ContentModel.TYPE_CONTENT);
        modelList.add(typeContent);

        final Model typeDirectory = modelService.buildModel(ContentModel.TYPE_DIRECTORY);
        modelList.add(typeDirectory);

        final Model propCreator = modelService.buildModel(ContentModel.PROP_CREATOR, PropertyType.STRING);
        modelList.add(propCreator);

        final Model propCreatorName = modelService.buildModel(ContentModel.PROP_CREATOR_NAME, PropertyType.STRING);
        modelList.add(propCreatorName);

        final Model propCreatedAt = modelService.buildModel(ContentModel.PROP_CREATED_AT, PropertyType.DATE);
        modelList.add(propCreatedAt);

        final Model propModifier = modelService.buildModel(ContentModel.PROP_MODIFIED_BY_ID, PropertyType.STRING);
        modelList.add(propModifier);

        final Model propModifiedBy = modelService.buildModel(ContentModel.PROP_MODIFIED_BY_NAME, PropertyType.STRING);
        modelList.add(propModifiedBy);

        final Model propModifiedAt = modelService.buildModel(ContentModel.PROP_MODIFIED_AT, PropertyType.DATE);
        modelList.add(propModifiedAt);

        final Model propName = modelService.buildModel(ContentModel.PROP_NAME, PropertyType.STRING) ;
        modelList.add(propName);

        final Model fruitAspect = modelService.buildModel("app-doc:fruit");
        modelList.add(fruitAspect);

        final Model fishAspect = modelService.buildModel("app-doc:fish");
        modelList.add(fishAspect);

        final Model fruitHeightPropertyModel = modelService.buildModel("fruit:weight", PropertyType.DOUBLE);
        modelList.add(fruitHeightPropertyModel);

        final Model fruitNamePropertyModel = modelService.buildModel("fruit:name", PropertyType.STRING);
        modelList.add(fruitNamePropertyModel);

        final Model fruitHavestDatePropertyModel = modelService.buildModel("fruit:harvest-date", PropertyType.DATE);
        modelList.add(fruitHavestDatePropertyModel);

        final Model fruitReferencePropertyModel = modelService.buildModel("fruit:reference", PropertyType.LONG);
        modelList.add(fruitReferencePropertyModel);

        modelList.forEach(modelService::save);
    }

}
