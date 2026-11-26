// Enregistrement de MS5 dans Eureka + decouverte des autres services (MS2-QUIZ)
const { Eureka } = require('eureka-js-client');

function createEurekaClient({ appName, host, port, eurekaUrls }) {
  return new Eureka({
    instance: {
      app: appName,
      instanceId: `${host}:${appName.toLowerCase()}:${port}`,
      hostName: host,
      ipAddr: '127.0.0.1',
      port: { $: port, '@enabled': true },
      vipAddress: appName.toLowerCase(),
      homePageUrl: `http://${host}:${port}/`,
      statusPageUrl: `http://${host}:${port}/info`, // lien du dashboard Eureka
      healthCheckUrl: `http://${host}:${port}/health`,
      dataCenterInfo: { '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo', name: 'MyOwn' },
    },
    eureka: {
      // les deux serveurs Eureka (8761, 8762)
      serviceUrls: { default: eurekaUrls.map((u) => (u.endsWith('/') ? u : `${u}/`) + 'apps/') },
      registryFetchInterval: 10000, // rafraichir la liste des instances toutes les 10 s
      maxRetries: 10,
      requestRetryDelay: 2000,
    },
  });
}

/**
 * Decouverte cote client : choisir une instance UP du service demande
 * (repartition aleatoire entre les instances, ex. MS2 sur 8082 et 8092).
 */
function resolveService(client, appId) {
  const instances = (client.getInstancesByAppId(appId) || []).filter((i) => i.status === 'UP');
  if (instances.length === 0) return null;
  const inst = instances[Math.floor(Math.random() * instances.length)];
  const port = typeof inst.port === 'object' ? inst.port.$ : inst.port;
  return `http://${inst.ipAddr || inst.hostName}:${port}`;
}

module.exports = { createEurekaClient, resolveService };
