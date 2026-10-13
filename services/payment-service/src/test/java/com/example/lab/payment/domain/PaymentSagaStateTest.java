package com.example.lab.payment.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PaymentSagaStateTest {
 @Test void transitionsHappyPath(){ Payment p=Payment.create("order",new BigDecimal("10.00"),"USD","key","fp"); p.initializeSaga(java.util.UUID.randomUUID(),1L,1,"NONE"); assertEquals("WAITING_ACCOUNTING",p.getSagaStatus()); p.sagaAccountingPosted(); assertEquals("WAITING_INVENTORY",p.getSagaStatus()); p.sagaInventoryReserved(); assertEquals("COMPLETED",p.getSagaStatus()); }
 @Test void transitionsCompensation(){ Payment p=Payment.create("order",new BigDecimal("10.00"),"USD","key2","fp"); p.initializeSaga(java.util.UUID.randomUUID(),1L,1,"INVENTORY_FAIL"); p.sagaAccountingPosted(); p.sagaCompensating(); assertEquals("COMPENSATING",p.getSagaStatus()); p.sagaCompensated(); assertEquals("COMPENSATED",p.getSagaStatus()); }
}
