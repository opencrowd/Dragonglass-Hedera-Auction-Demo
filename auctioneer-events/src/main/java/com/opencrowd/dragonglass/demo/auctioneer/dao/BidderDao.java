package com.opencrowd.dragonglass.demo.auctioneer.dao;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BidderDao {

  @Autowired
  private RestTemplate restTemplate;

  public Map<String, String> getAllBiddersAccountsForHCS(){
      return restTemplate.getForEntity("http://localhost:8081/hcs/auction/bidders", Map.class).getBody();
  }

}
