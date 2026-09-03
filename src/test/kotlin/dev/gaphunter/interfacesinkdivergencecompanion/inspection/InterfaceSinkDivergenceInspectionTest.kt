package dev.gaphunter.interfacesinkdivergencecompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Every test method uses its own uniquely-suffixed class/interface names -- same discipline as this catalog's other cross-implementation test suites (avoids ambiguous cross-file class resolution). */
class InterfaceSinkDivergenceInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(InterfaceSinkDivergenceInspection::class.java)
    }

    fun `test implementations that diverge on logging the argument are flagged`() {
        myFixture.configureByText(
            "GatewayA.java",
            """
            import org.springframework.web.bind.annotation.PostMapping;

            interface PaymentGatewayA {
                void charge(String cardToken);
            }

            class LoggingGatewayA implements PaymentGatewayA {
                private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingGatewayA.class);
                public void charge(String cardToken) {
                    log.info(cardToken);
                }
            }

            class QuietGatewayA implements PaymentGatewayA {
                public void charge(String cardToken) {
                    // never logs
                }
            }

            class CheckoutA {
                @PostMapping("/pay")
                void handle(PaymentGatewayA gateway, String cardToken) {
                    gateway.charge(cardToken);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("PaymentGatewayA") == true && it.description?.contains("never does") == true })
    }

    fun `test implementations that both log the argument consistently are not flagged`() {
        myFixture.configureByText(
            "GatewayB.java",
            """
            import org.springframework.web.bind.annotation.PostMapping;

            interface PaymentGatewayB {
                void charge(String cardToken);
            }

            class LoggingGatewayB1 implements PaymentGatewayB {
                private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingGatewayB1.class);
                public void charge(String cardToken) {
                    log.info(cardToken);
                }
            }

            class LoggingGatewayB2 implements PaymentGatewayB {
                private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingGatewayB2.class);
                public void charge(String cardToken) {
                    log.warn(cardToken);
                }
            }

            class CheckoutB {
                @PostMapping("/pay")
                void handle(PaymentGatewayB gateway, String cardToken) {
                    gateway.charge(cardToken);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("PaymentGatewayB") == true })
    }

    fun `test implementations that never log are not flagged`() {
        myFixture.configureByText(
            "GatewayC.java",
            """
            import org.springframework.web.bind.annotation.PostMapping;

            interface PaymentGatewayC {
                void charge(String cardToken);
            }

            class QuietGatewayC1 implements PaymentGatewayC {
                public void charge(String cardToken) {}
            }

            class QuietGatewayC2 implements PaymentGatewayC {
                public void charge(String cardToken) {}
            }

            class CheckoutC {
                @PostMapping("/pay")
                void handle(PaymentGatewayC gateway, String cardToken) {
                    gateway.charge(cardToken);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("PaymentGatewayC") == true })
    }

    fun `test a call through the CONCRETE type is not flagged`() {
        myFixture.configureByText(
            "GatewayD.java",
            """
            import org.springframework.web.bind.annotation.PostMapping;

            interface PaymentGatewayD {
                void charge(String cardToken);
            }

            class LoggingGatewayD implements PaymentGatewayD {
                private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingGatewayD.class);
                public void charge(String cardToken) {
                    log.info(cardToken);
                }
            }

            class QuietGatewayD implements PaymentGatewayD {
                public void charge(String cardToken) {}
            }

            class CheckoutD {
                @PostMapping("/pay")
                void handle(LoggingGatewayD gateway, String cardToken) {
                    gateway.charge(cardToken);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("PaymentGatewayD") == true })
    }

    fun `test an interface with only one real implementation is not flagged`() {
        myFixture.configureByText(
            "GatewayE.java",
            """
            import org.springframework.web.bind.annotation.PostMapping;

            interface PaymentGatewayE {
                void charge(String cardToken);
            }

            class OnlyGatewayE implements PaymentGatewayE {
                private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OnlyGatewayE.class);
                public void charge(String cardToken) {
                    log.info(cardToken);
                }
            }

            class CheckoutE {
                @PostMapping("/pay")
                void handle(PaymentGatewayE gateway, String cardToken) {
                    gateway.charge(cardToken);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("PaymentGatewayE") == true })
    }
}
