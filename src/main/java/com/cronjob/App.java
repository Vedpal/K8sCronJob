package com.cronjob;

import com.cronjob.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobBuilder;
import io.fabric8.kubernetes.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    public static void main( String[] args )
    {
        System.out.println( "update-domain-lists-from-config-test" );
        Config config = new ConfigBuilder().withNamespace("default").build();

        try(final KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build()) {
            final String namespace = client.getNamespace();

            List<EnvVar> envVarList = new ArrayList<>();
            EnvVar envVar1 = new EnvVar();
            envVar1.setName("API");
            envVar1.setValue("https://wwwegenciaeu.int-maui.sb.karmalab.net/config-external-import-service/v1/external-configs/trigger-refresh");
            envVarList.add(envVar1);

            EnvVar envVar2 = new EnvVar();
            envVar2.setName("METHOD");
            envVar2.setValue("POST");
            envVarList.add(envVar2);

            EnvVar envVar3 = new EnvVar();
            envVar3.setName("AUTHTOKEN_REQUIRED");
            envVar3.setValue("TRUE");
            envVarList.add(envVar3);

            EnvVar envVar4 = new EnvVar();
            envVar4.setName("BASIC_KEY");
            envVar4.setValue("YTI5ZWU3MmUtZTY0Ni00ZDkwLTkwNWMtNTk2OWNmM2U2NmE5OlBmam9jbFZoaUpTNDJSelFNVDJqal9qcVNxU0ZYdUtf");
            envVarList.add(envVar4);

            CronJob cronJob1 = new CronJobBuilder()
                    .withApiVersion("batch/v1")
                    .withNewMetadata()
                    .withName("update-domain-lists-from-config-test")
                    .withLabels(Collections.singletonMap("config", "import-service"))
                    .endMetadata()
                    .withNewSpec()
                    .withSchedule("*/1 * * * *")
                    .withNewJobTemplate()
                    .withNewSpec()
                    .withNewTemplate()
                    .withNewSpec()
                    .addNewContainer()
                    .withName("update-domain-lists-from-config-external-import-service-test")
                    .withImage("egencia-docker-virtual.egencialab.labartifactory.alm.expedia.biz/schedule-endpoint-executor:latest")
                    .withEnv(envVarList)
                    .withArgs("payload={}")
                    .endContainer()
                    .withRestartPolicy("OnFailure")
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .endJobTemplate()
                    .endSpec()
                    .build();

            logger.info("Creating cron job from object");
            cronJob1 = client.batch().v1().cronjobs().inNamespace(namespace).create(cronJob1);
            logger.info("Successfully created cronjob with name {}", cronJob1.getMetadata().getName());
            System.out.println("Successfully created cronjob with name {}" + cronJob1.getMetadata().getName());

            logger.info("Watching over pod which would be created during cronjob execution...");
            final CountDownLatch watchLatch = new CountDownLatch(1);
            try (Watch watch = client.pods().inNamespace(namespace).withLabel("job-name").watch(new Watcher<Pod>() {
                @Override
                public void eventReceived(Action action, Pod aPod) {
                    logger.info(aPod.getMetadata().getName(), aPod.getStatus().getPhase());
                    if(aPod.getStatus().getPhase().equals("Succeeded")) {
                        logger.info("Logs -> {}", client.pods().inNamespace(namespace).withName(aPod.getMetadata().getName()).getLog());
                        watchLatch.countDown();
                    }
                }

                @Override
                public void onClose(WatcherException e) {
                    // Ignore
                }
            })) {
                watchLatch.await(2, TimeUnit.MINUTES);
            } catch (KubernetesClientException | InterruptedException e) {
                logger.info("Could not watch pod", e);
                System.out.println("Could not watch pod" + e);
            }
        } catch (KubernetesClientException exception) {
            logger.info("An error occurred while processing cronjobs:{}", exception.getMessage());
            System.out.println("An error occurred while processing cronjobs:{}" + exception.getMessage());
        }
    }
}
