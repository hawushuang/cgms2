/*
 * Module:	LED driver
 * Author:	Lvjianfeng
 * Date:	2011.10
 */


#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include "drv_led.h"


//Constant definition


//Type definition


//Private variable definition

static int m_i_RedBrightnessMax = {0};
static int m_i_GreenBrightnessMax = {0};

static const char *const m_i8_RedBrightness = "/sys/class/leds/led0-red/brightness";
static const char *const m_i8_RedBrightnessMax = "/sys/class/leds/led0-red/max_brightness";
static const char *const m_i8_GreenBrightness = "/sys/class/leds/led0-green/brightness";
static const char *const m_i8_GreenBrightnessMax = "/sys/class/leds/led0-green/max_brightness";


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

	i_FileDescriptor = open(m_i8_GreenBrightnessMax, O_RDONLY);

	if (i_FileDescriptor < 0)
	{
		LOGE("failed to open %s\n", m_i8_GreenBrightnessMax);

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
		m_i_GreenBrightnessMax = strtol(i8_Buffer, (char **) NULL, 10);
		LOGD("max green led brightness %d\n", m_i_GreenBrightnessMax);
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

		i_LEDBrightness = m_i_RedBrightnessMax * i_Brightness / 100;
	}
	else if (i_Color == DRV_LED_COLOR_GREEN)
	{
		i_FileDescriptor = open(m_i8_GreenBrightness, O_RDWR);

		if (i_FileDescriptor < 0)
		{
			LOGE("failed to open %s\n", m_i8_GreenBrightness);
			return;
		}

		i_LEDBrightness = m_i_GreenBrightnessMax * i_Brightness / 100;
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
	else if (i_Color == DRV_LED_COLOR_GREEN)
	{
		i_FileDescriptor = open(m_i8_GreenBrightness, O_RDONLY);

		if (i_FileDescriptor < 0)
		{
			LOGE("failed to open %s\n", m_i8_GreenBrightness);
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
		i_Brightness = i_Brightness * 100 / m_i_GreenBrightnessMax;
	}

	return i_Brightness;
}


#if DRV_LED_TEST_ENABLE == 1

void DrvLED_Test(void)
{
}

#endif


//Private function definition
