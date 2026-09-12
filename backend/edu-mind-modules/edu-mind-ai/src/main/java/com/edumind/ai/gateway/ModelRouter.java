package com.edumind.ai.gateway;

public interface ModelRouter {

    String resolveModelKey(String scene, String explicitModelKey);
}
