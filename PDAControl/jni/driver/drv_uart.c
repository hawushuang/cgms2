/*
 * Module:	UART driver
 * Author:	Lvjianfeng
 * Date:	2011.9
 */


#include <termios.h>
#include <fcntl.h>
#include <stdio.h>

#include "lib_frame.h"

#include "drv_uart.h"


//Constant definition

#define UART_BUFFER_SIZE			128
#define UART_BAUDRATE				B115200
#define WAKEUP_INPUT_CHECK_CYCKE	100
#define WAKEUP_INPUT_TIMEOUT		100000
#define WAKEUP_INPUT_DELAY			10000

#define DF_GPIO_SET_LOW				_IOW('Q', 0x0, int)
#define DF_GPIO_SET_HIGH			_IOW('Q', 0x1, int)
#define DF_GPIO_GET_VALUE			_IOW('Q', 0x2, int)
#define GPIO_TO_PIN(bank, gpio) 	(32 * (bank) + (gpio))
#define AM335X_CC2540_1_IRQ_N    	GPIO_TO_PIN(0, 17)
#define AM335X_CC2540_2_IRQ_N    	GPIO_TO_PIN(0, 16)

#define FRAME_HEADER				'+'
#define FRAME_ESCAPE				'/'


//Type definition


//Private variable definition

static uint m_ui_IsSending = {0};
static uint m_ui_IsReceiving = {0};
static uint m_ui_ReadLength = {0};
static uint8 m_u8_BufferWrite[UART_BUFFER_SIZE] = {0};
static uint8 m_u8_BufferRead[UART_BUFFER_SIZE] = {0};
static uint8 m_u8_BufferLog[UART_BUFFER_SIZE] = {0};
static drv_uart_callback m_t_Callback = {0};
static lib_frame_object m_t_Frame = {0};

static int m_i_FileUART = -1;
static int m_i_FileWakeup = -1;
static fd_set m_t_FileDescriptorSet = {0};

static const char m_i8_UARTFilePath[] = "/dev/ttyO4";
static const char m_i8_WakeupFilePath[] = "/dev/cc2540_gpio";


//Private function declaration

static uint DrvUART_InitializeWakeup(void);
static uint DrvUART_InitializeUART(void);
static int DrvUART_SetWakeup(uint ui_Level);
static uint DrvUART_GetWakeup(void);


//Public function definition

uint DrvUART_Initialize(void)
{
	if (DrvUART_InitializeWakeup() != FUNCTION_OK)
	{
		return FUNCTION_FAIL;
	}

	if (DrvUART_InitializeUART() != FUNCTION_OK)
	{
		return FUNCTION_FAIL;
	}

	m_t_Frame.u8_Header = FRAME_HEADER;
	m_t_Frame.u8_Escape = FRAME_ESCAPE;
	LibFrame_Initialize(&m_t_Frame);

	return FUNCTION_OK;
}


void DrvUART_Finalize(void)
{
	LOGD("Close UART device file %d", m_i_FileUART);

	if (m_i_FileUART != -1)
	{
		close(m_i_FileUART);
	}

	LOGD("Close wakeup device file %d", m_i_FileWakeup);

	if (m_i_FileWakeup != -1)
	{
		close(m_i_FileWakeup);
	}
}


uint DrvUART_SetConfig
(
	uint ui_Parameter,
	const void *vp_Value
)
{
	switch (ui_Parameter)
	{
		case DRV_UART_PARAM_CALLBACK:
			m_t_Callback = *((const drv_uart_callback *)vp_Value);
			break;

		default:
			break;
	}

	return FUNCTION_OK;
}


uint DrvUART_GetConfig
(
	uint ui_Parameter,
	void *vp_Value
)
{
	switch (ui_Parameter)
	{
		case DRV_UART_PARAM_BUSY:
			*((uint *)vp_Value) = 0;
			break;

		default:
			return FUNCTION_FAIL;
	}

	return FUNCTION_OK;
}


uint DrvUART_Write
(
	const uint8 *u8p_Data,
	uint ui_Length
)
{
	uint ui_Log;
	uint ui_Index;
	uint ui_Timeout;
	lib_frame_int t_Length;
	char *u8p_BufferLog;


	//Check if length of data is out of range
	if ((ui_Length <= 0) || (ui_Length > (UART_BUFFER_SIZE / 2) -
		LIB_FRAME_HEADER_LENGTH))
	{
		return FUNCTION_FAIL;
	}

	LOGD("Write frame, length: %d", ui_Length);

	if (m_ui_IsReceiving == 0)
	{
		if (DrvUART_SetWakeup(1) < 0)
		{
			LOGE("Write wakeup output fail");
		}
	}

	m_ui_IsSending = 1;
	ui_Timeout = 0;

	if (DrvUART_SetWakeup(0) < 0)
	{
		LOGE("Write wakeup output fail");
	}

	do
	{
		ui_Timeout += WAKEUP_INPUT_CHECK_CYCKE;
		usleep(WAKEUP_INPUT_CHECK_CYCKE);
	}
	while ((m_ui_IsReceiving == 0) && (ui_Timeout < WAKEUP_INPUT_TIMEOUT) &&
		(DrvUART_GetWakeup() > 0));

	if ((m_ui_IsReceiving == 0) && (ui_Timeout >= WAKEUP_INPUT_TIMEOUT))
	{
		LOGE("Wakeup input timeout");

		if (DrvUART_SetWakeup(1) < 0)
		{
			LOGE("Write wakeup output fail");
		}

		m_ui_IsSending = 0;

		return FUNCTION_FAIL;
	}

	ui_Log = 0;
	u8p_BufferLog = (char *)m_u8_BufferLog;

	for (ui_Index = 0; ui_Index < ui_Length; ui_Index++)
	{
		ui_Log = snprintf(u8p_BufferLog, (char *)m_u8_BufferLog +
			sizeof(m_u8_BufferLog) - u8p_BufferLog, "%02X ", u8p_Data[ui_Index]);
		u8p_BufferLog += ui_Log;
	}

	LOGD("Write frame, data: %s", m_u8_BufferLog);

	t_Length = (lib_frame_int)ui_Length;
	LibFrame_Pack(&m_t_Frame, u8p_Data, m_u8_BufferWrite, &t_Length);

	if (write(m_i_FileUART, (const void *)m_u8_BufferWrite, t_Length) < 0)
	{
		LOGE("Write serial port fail");

		if (DrvUART_SetWakeup(1) < 0)
		{
			LOGE("Write wakeup output fail");
		}

		m_ui_IsSending = 0;

		return FUNCTION_FAIL;
	}

	fsync(m_i_FileUART);
	tcdrain(m_i_FileUART);

	if (DrvUART_SetWakeup(1) < 0)
	{
		LOGE("Write wakeup output fail");
	}

	do
	{
		ui_Timeout += WAKEUP_INPUT_CHECK_CYCKE;
		usleep(WAKEUP_INPUT_CHECK_CYCKE);
	}
	while ((m_ui_IsReceiving > 0) && (ui_Timeout < WAKEUP_INPUT_DELAY));

	if ((m_ui_IsReceiving > 0) && (ui_Timeout < WAKEUP_INPUT_DELAY))
	{
		if (DrvUART_SetWakeup(0) < 0)
		{
			LOGE("Write wakeup output fail");
		}
	}

	m_ui_IsSending = 0;

	LOGD("Write frame end");

	return FUNCTION_OK;
}


uint DrvUART_Read
(
	uint8 *u8p_Data,
	uint *uip_Length
)
{
	LOGD("Read frame, length: %d", *uip_Length);

	if ((*uip_Length == 0) || (m_ui_ReadLength == 0))
	{
		return FUNCTION_FAIL;
	}

	if (*uip_Length > m_ui_ReadLength)
	{
		*uip_Length = m_ui_ReadLength;
	}

	if (m_t_Callback.fp_Memcpy != 0)
	{
		m_t_Callback.fp_Memcpy(u8p_Data, m_u8_BufferRead, *uip_Length);
	}

	m_ui_ReadLength = 0;

	return FUNCTION_OK;
}


void DrvUART_Interrupt(void)
{
	uint ui_Log;
	uint ui_Index;
	uint8 u8_Data;
	lib_frame_int t_Length;
	char *u8p_BufferLog;
	struct timeval t_Timeout;


	if (DrvUART_GetWakeup() > 0)
	{
		m_ui_IsReceiving = 0;

		if (m_ui_IsSending == 0)
		{
			if (DrvUART_SetWakeup(1) < 0)
			{
				LOGE("Write wakeup output fail");
			}
		}
	}
	else
	{
		m_ui_IsReceiving = 1;

		if (m_ui_IsSending == 0)
		{
			if (DrvUART_SetWakeup(0) < 0)
			{
				LOGE("Write wakeup output fail");
			}
		}
	}

	t_Timeout.tv_sec = 0;
	t_Timeout.tv_usec = 20000;
	FD_ZERO(&m_t_FileDescriptorSet);
	FD_SET(m_i_FileUART, &m_t_FileDescriptorSet);

	if (select(m_i_FileUART + 1, &m_t_FileDescriptorSet, NULL ,NULL,
		&t_Timeout) > 0)
	{
		if (FD_ISSET(m_i_FileUART, &m_t_FileDescriptorSet))
		{
			if (m_ui_ReadLength == 0)
			{
				t_Length = (lib_frame_int)read(m_i_FileUART, (void *)&u8_Data,
					1);
				m_ui_ReadLength = (uint)LibFrame_Unpack(&m_t_Frame, &u8_Data,
					m_u8_BufferRead, &t_Length);

				if (m_ui_ReadLength > 0)
				{
					LOGD("Receive frame, length: %d", m_ui_ReadLength);

					ui_Log = 0;
					u8p_BufferLog = (char *)m_u8_BufferLog;

					for (ui_Index = 0; ui_Index < m_ui_ReadLength; ui_Index++)
					{
						ui_Log = snprintf(u8p_BufferLog, (char *)m_u8_BufferLog +
							sizeof(m_u8_BufferLog) - u8p_BufferLog, "%02X ",
							m_u8_BufferRead[ui_Index]);
						u8p_BufferLog += ui_Log;
					}

					LOGD("Receive frame, data: %s", m_u8_BufferLog);

					if (m_t_Callback.fp_ReadDone != 0)
					{
						m_t_Callback.fp_ReadDone(m_u8_BufferRead,
							m_ui_ReadLength);
					}

					LibFrame_Initialize(&m_t_Frame);

					LOGD("Receive frame end");
				}
				else
				{
					if (m_t_Frame.t_Length >= UART_BUFFER_SIZE)
					{
						LOGD("Data received overflow");
						LibFrame_Initialize(&m_t_Frame);
					}
				}
			}
		}
	}
}


#if DRV_UART_TEST_ENABLE == 1

void DrvUART_Test(void)
{
}

#endif


//Private function definition

static uint DrvUART_InitializeWakeup(void)
{
	if (m_i_FileWakeup != -1)
	{
		close(m_i_FileWakeup);
	}

	//Open wakeup output device file in write mode
	m_i_FileWakeup = open(m_i8_WakeupFilePath, O_RDWR);

	LOGD("Open wakeup device file %d", m_i_FileWakeup);

	if (m_i_FileWakeup == -1)
	{
		LOGE("Cannot open wakeup");

		return FUNCTION_FAIL;
	}

	DrvUART_SetWakeup(1);

	return FUNCTION_OK;
}


static uint DrvUART_InitializeUART(void)
{
	struct termios t_Config;


	if (m_i_FileUART != -1)
	{
		close(m_i_FileUART);
	}

	//Open UART device file in read and write mode
	m_i_FileUART = open(m_i8_UARTFilePath, O_RDWR | O_NOCTTY | O_NDELAY);

	LOGD("Open UART device file %d", m_i_FileUART);

	if (m_i_FileUART == -1)
	{
		LOGE("Cannot open UART");

		return FUNCTION_FAIL;
	}

	LOGD("Configuring serial port");

	//Get UART attributes
	if (tcgetattr(m_i_FileUART, &t_Config))
	{
		LOGE("Get UART attributes failed");
		close(m_i_FileUART);

		return FUNCTION_FAIL;
	}

	//Configure UART baud rate
	cfmakeraw(&t_Config);
	cfsetispeed(&t_Config, UART_BAUDRATE);
	cfsetospeed(&t_Config, UART_BAUDRATE);

	//Set UART attributes
	if (tcsetattr(m_i_FileUART, TCSANOW, &t_Config))
	{
		LOGE("Set UART attributes failed");
		close(m_i_FileUART);

		return FUNCTION_FAIL;
	}

	FD_ZERO(&m_t_FileDescriptorSet);
	FD_SET(m_i_FileUART, &m_t_FileDescriptorSet);

	return FUNCTION_OK;
}


static int DrvUART_SetWakeup(uint ui_Level)
{
	if (ui_Level > 0)
	{
		return ioctl(m_i_FileWakeup, DF_GPIO_SET_HIGH,
			AM335X_CC2540_1_IRQ_N);
	}
	else
	{
		return ioctl(m_i_FileWakeup, DF_GPIO_SET_LOW,
			AM335X_CC2540_1_IRQ_N);
	}
}


static uint DrvUART_GetWakeup(void)
{
	return (uint)ioctl(m_i_FileWakeup, DF_GPIO_GET_VALUE,
		AM335X_CC2540_2_IRQ_N);
}
