package com.webstart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webstart.DTO.*;
import com.webstart.model.*;
import com.webstart.repository.FeatureofinterestJpaRepository;
import com.webstart.repository.ObservablePropertyJpaRepository;
import com.webstart.repository.ObservationJpaRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.sql.Timestamp;
import java.util.*;


@Service("observationProperyService")
@Transactional
public class ObservationPropertyServiceImpl implements ObservationProperyService {

    @Autowired
    ObservablePropertyJpaRepository observablePropertyJpaRepository;
    @Autowired
    ObservationJpaRepository observationJpaRepository;
    @Autowired
    FeatureofinterestJpaRepository featureofinterestJpaRepository;

    public JSONObject getAllObsPropeties() {

        JSONObject finalobj = new JSONObject();
        JSONArray list = new JSONArray();

        List<ObservableProperty> obsPropertiesList = observablePropertyJpaRepository.findAll();


        for (ObservableProperty observableProperty : obsPropertiesList) {
            JSONObject obj = new JSONObject();
            obj.put("observablepropertyid", observableProperty.getObservablePropertyId());
            obj.put("description", observableProperty.getDescription());
            list.add(obj);
        }

        finalobj.put("obsprop", list);

        return finalobj;
    }


    public String getObservationsData(Long obspropId, int userId, String identifier, Date from, Date to) {
        String jsonInString = null;

        try {
            java.sql.Timestamp timeFrom = new java.sql.Timestamp(from.getTime());
            java.sql.Timestamp timeTo = new java.sql.Timestamp(to.getTime());

            List<Object[]> listofObjs = observationJpaRepository.findMeasureByObsPropId(obspropId, userId, identifier, timeFrom, timeTo);

            if (listofObjs.size() == 0) {
                return null;
            }

            ObservableMeasure obsMeasure = new ObservableMeasure();
            Object[] obj = listofObjs.get(0);

            obsMeasure.setIdentifier(String.valueOf(listofObjs.get(0)[0]));
            obsMeasure.setObservableProperty(String.valueOf(listofObjs.get(0)[1]));
            obsMeasure.setUnit(String.valueOf(listofObjs.get(0)[4]));
            List<ValueTime> ls = new ArrayList<ValueTime>();

            Iterator itr = listofObjs.iterator();
            while (itr.hasNext()) {
                Object[] objec = (Object[]) itr.next();
                //Object[] objValueTime = new Object[2];

                Timestamp tTime = (java.sql.Timestamp) objec[2];

                ls.add(new ValueTime(tTime.getTime() / 1000L, (BigDecimal) objec[3], tTime));
            }

            obsMeasure.setMeasuredata(ls);
            ObjectMapper mapper = new ObjectMapper();

            //Object to JSON in String
            jsonInString = mapper.writeValueAsString(obsMeasure);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

        return jsonInString;
    }

    public Long getObservationsCounter(Long obspropId, int userId, String identifier, Date from, Date to) {
        return observationJpaRepository.findMeasuresCount(obspropId, userId, identifier, new java.sql.Timestamp(from.getTime()), new java.sql.Timestamp(to.getTime()));
    }

    public ObservableMeasure getObservationData(Long obspropId, int userId, String identifier, Date from, Date to) {
        ObservableMeasure obsMeasure = new ObservableMeasure();

        try {
            List<Object[]> listofObjs = observationJpaRepository.findMeasureByObsPropId(obspropId, userId, identifier, new java.sql.Timestamp(from.getTime()), new java.sql.Timestamp(to.getTime()));

            if (listofObjs.size() == 0) {
                return null;
            }

            Object[] obj = listofObjs.get(0);
            obsMeasure.setIdentifier(String.valueOf(obj[0]));
            obsMeasure.setObservableProperty(String.valueOf(obj[1]));
            obsMeasure.setUnit(String.valueOf(obj[4]));

            List<ValueTime> ls = new ArrayList<ValueTime>();
            Iterator itr = listofObjs.iterator();

            while (itr.hasNext()) {
                Object[] objec = (Object[]) itr.next();
                //Object[] objValueTime = new Object[2];
                Timestamp tTime = (java.sql.Timestamp) objec[2];
                ls.add(new ValueTime(tTime.getTime() / 1000L, (BigDecimal) objec[3], tTime));
            }

            obsMeasure.setMeasuredata(ls);
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }

        return obsMeasure;
    }

    public String getLastObservationsDate(int userId) {
        String jsonInString;

        try {
            Timestamp lastdate = observationJpaRepository.findlastdatetime(userId);
            ObjectMapper mapper = new ObjectMapper();
            jsonInString = mapper.writeValueAsString(lastdate);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return jsonInString;
    }

    public String getLastObservationbyIdentifier(int userId, String identifier) {
        String jsonInString = null;

        try {
            Timestamp lastdate = observationJpaRepository.findlastdatetime(userId);
            List<Object[]> listMeasures = observationJpaRepository.findLastMeasures(userId, identifier, lastdate);

            if (listMeasures.size() == 0) {
                return null;
            }

            List<ObservationMeasure> ls = new ArrayList<ObservationMeasure>();
            Iterator itr = listMeasures.iterator();

            while (itr.hasNext()) {
                Object[] objec = (Object[]) itr.next();
                Timestamp tTime = (java.sql.Timestamp) objec[1];
                ls.add(new ObservationMeasure(tTime.getTime() / 1000L, (BigDecimal) objec[2], tTime, objec[3].toString(), objec[0].toString()));
            }

            ObjectMapper mapper = new ObjectMapper();       //Object to JSON in String
            jsonInString = mapper.writeValueAsString(ls);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

        return jsonInString;
    }

    public void setObservationMinmaxValues(List<FeatureMinMaxValue> observationMinmaxList) {
        try {
            for (FeatureMinMaxValue featureMinMaxValue : observationMinmaxList) {
                featureofinterestJpaRepository.setObservableMinmax(featureMinMaxValue.getObspropertyid(), featureMinMaxValue.getMinval(), featureMinMaxValue.getMaxval());
            }
        } catch (Exception exc) {

        }

    }

}


