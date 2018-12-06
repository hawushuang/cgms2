/*
 * Module:	LED driver
 * Author:	Lvjianfeng
 * Date:	2011.10
 */


#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>

#include "drv_led.h"


//Constant definition


//Type definition


//Private variable definition

static int m_i_RedBrightnessMax = {0};
static int m_i_YellowBrightnessMax = {0};

static const char *const m_i8_RedBrightness = "/sys/class/leds/red/brightness";
static const char *const m_i8_RedBrightnessMax = "/sys/class/leds/red/max_brightness";
static const char *const m_i8_YellowBrightness = "/sys/class/leds/yellow/brightness";
static const char *const m_i8_YellowBrightnessMax = "/sys/class/leds/yellow/max_brightness";


//Private function declaration


//Public function definition

uint DrvLED_Initialize(void)
{
	char i8_Buffer[20];
	int i_FileDescriptor;
	ssize_t t_Length;


	i_FileDescriptor = open(m_i8_RedBrightnessMax, O_RDONLY);

	if (i_FileDescriptor < 0)
	{
		LOGE("failed to open %s\n", m_i8_RedBrightnessMax);

		return FUNCTION_FAIL;
	}

	memset(i8_Buffer, 0, sizeof(i8_Buffer));
	t_Length = read(i_FileDescriptor, i8_Buffer, sizeof(i8_Buffer));
	close(i_FileDescriptor);

	if (t_Length < 0)
	{
		LOGE("fail to read max red led brightness:%s\n", i8_Buffer);

		return FUNCTION_FAIL;
	}
	else
	{
		m_i_RedBrightnessMax = strtol(i8_Buffer, (char **) NULL, 10);
		LOGD("max red led brightness %d\n", m_i_RedBrightnessMax);
	}

	i_FileDescriptor = open(m_i8_YellowBrightnessMax, O_RDONLY);

	if (i_FileDescriptor < 0)
	{
		LOGE("failed to open %s\n", m_i8_YellowBrightnessMax);

		return FUNCTION_FAIL;
	}

	memset(i8_Buffer, 0, sizeof(i8_Buffer));
	t_Length = read(i_FileDescriptor, i8_Buffer, sizeof(i8_Buffer));
	close(i_FileDescriptor);

	if (t_Length < 0)
	{
		LOGE("fail to read max green led brightness:%s\n", i8_Buffer);

		return FUNCTION_FAIL;
	}
	else
	{
		m_i_YellowBrightnessMax = strtol(i8_Buffer, (char **) NULL, 10);
		LOGD("max green led brightness %d\n", m_i_YellowBrightnessMax);
	}

	return FUNCTION_OK;
}


void DrvLED_Set
(
	int i_Color,
	int i_Brightness
)
{
	char i8_Buffer[20];
	int i_FileDescriptor;
	int i_LEDBrightness;
	ssize_t t_Length;


	if (i_Brightness > 100)
	{
		i_Brightness = 100;
	}
	else if (i_Brightness < 0)
	{
		i_Brightness = 0;
	}

	if (i_Color == DRV_LED_COLOR_RED)
	{
		i_FileDescriptor = open(m_i8_RedBrightness, O_RDWR);

		if (i_FileDescriptor < 0)
		{
			LOGE("failed to open %s\n", m_i8_RedBrightness);
			return;
		}

		i_LEDBrightness = m_i_RedBrightnessMax * (100 - i_Brightness) / 100;
	}
	else if (i_Color == DRV_LED_COLOR_YELLOW)
	{
		i_FileDescriptor = open(m_i8_YellowBrightness, O_RDWR);

		if (i_FileDescriptor < 0)
		{
			LOGE("failed to open %s\n", m_i8_YellowBrightness);
			return;
		}

		i_LEDBrightness = m_i_YellowBrightnessMax * (100 - i_Brightness) / 100;
	}
	else
	{
		return;
	}

	memset(i8_Buffer, 0, sizeof(i8_Buffer));
	t_Length = write(i_FileDescriptor, i8_Buffer,
		(size_t)sprintf(i8_Buffer, "%d", i_LEDBrightness));
	close(i_FileDescriptor);

	if (t_Length < 0)
	{
		LOGE("fail to write brightness:%s\n", i8_Buffer);
	}
}


int DrvLED_Get
(
	int i_Color
)
{
	char i8_Buffer[20];
	int i_FileDescriptor;
	int i_Brightness;
	ssize_t t_Length;


	if (i_Color == DRV_LED_COLOR_RED)
	{
		i_FileDescriptor = open(m_i8_RedBrightness, O_RDONLY);

		if (i_FileDescriptor < 0)
		{
			LOGE("failed to open %s\n", m_i8_RedBrightness);
			return -1;
		}
	}
	else if (i_Color == DRV_LED_COLOR_YELLOW)
	{
		i_FileDescriptor = open(m_i8_YellowBrightness, O_RDONLY);

		if (i_FileDescriptor < 0)
		{
			LOGE("failed to open %s\n", m_i8_YellowBrightness);
			return -1;
		}
	}
	else
	{
		return -1;
	}

	memset(i8_Buffer, 0, sizeof(i8_Buffer));
	t_Length = read(i_FileDescriptor, i8_Buffer, sizeof(i8_Buffer));
	close(i_FileDescriptor);

	if (t_Length < 0)
	{
		LOGE("fail to read led brightness:%s\n", i8_Buffer);

		return -1;
	}

	i_Brightness = strtol(i8_Buffer, (char **) NULL, 10);

	if (i_Color == DRV_LED_COLOR_RED)
	{
		i_Brightness = i_Brightness * 100 / m_i_RedBrightnessMax;
	}
	else
	{
		i_Brightness = i_Brightness * 100 / m_i_YellowBrightnessMax;
	}

	if (i_Brightness > 100)
	{
		i_Brightness = 0;
	}
	else
	{
		i_Brightness = 100 - i_Brightness;
	}

	return i_Brightness;
}


#if DRV_LED_TEST_ENABLE == 1

void DrvLED_Test(void)
{
}

#endif


//Private function definition
