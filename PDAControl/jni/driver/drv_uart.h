/*
 * Module:	UART driver
 * Author:	Lvjianfeng
 * Date:	2011.9
 */

#ifndef _DRV_UART_H_
#define _DRV_UART_H_


#include "global.h"


//Constant define

#ifndef DRV_UART_TEST_ENABLE
#define DRV_UART_TEST_ENABLE		0
#endif


//Type definition

typedef enum
{
	DRV_UART_PARAM_CALLBACK = 0,
	DRV_UART_PARAM_BUSY,
	DRV_UART_COUNT_PARAM
} drv_uart_param;

typedef void (*drv_uart_callback_write_done)(void);

typedef void (*drv_uart_callback_read_done)
(
	const uint8 *u8p_Data,
	uint ui_Length
);

typedef void (*drv_uart_callback_memcpy)
(
	uint8 *u8p_Target,
	const uint8 *u8p_Source,
	uint ui_Lenght
);

typedef struct
{
	drv_uart_callback_write_done fp_WriteDone;
	drv_uart_callback_read_done fp_ReadDone;
	drv_uart_callback_memcpy fp_Memcpy;
} drv_uart_callback;


//Function declaration

uint DrvUART_Initialize(void);
void DrvUART_Finalize(void);
uint DrvUART_SetConfig
(
	uint ui_Parameter,
	const void *vp_Value
);
uint DrvUART_GetConfig
(
	uint ui_Parameter,
	void *vp_Value
);
uint DrvUART_Write
(
	const uint8 *u8p_Data,
	uint ui_Length
);
uint DrvUART_Read
(
	uint8 *u8p_Data,
	uint *uip_Length
);
void DrvUART_Interrupt(void);

#if DRV_UART_TEST_ENABLE == 1
void DrvUART_Test(void);
#endif

#endif
