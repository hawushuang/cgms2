/*
 * Module:	LED driver
 * Author:	Lvjianfeng
 * Date:	2011.10
 */

#ifndef _DRV_LED_H_
#define _DRV_LED_H_


#include "global.h"


//Constant definition

#define DRV_LED_TEST_ENABLE		0


//Type definition

typedef enum
{
	DRV_LED_COLOR_RED = 0,
	DRV_LED_COLOR_GREEN
} drv_led_color;


//Function declaration

uint DrvLED_Initialize(void);
void DrvLED_Set
(
	int i_Color,
	int i_Brightness
);
int DrvLED_Get
(
	int i_Color
);

#if DRV_LED_TEST_ENABLE == 1
void DrvLED_Test(void);
#endif

#endif
