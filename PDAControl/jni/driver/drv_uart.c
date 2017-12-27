/*
 * Module:	UART driver
 * Author:	Lvjianfeng
 * Date:	2011.9
 */


#include <termios.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

#include "lib_frame.h"

#include "drv_uart.h"


//Constant definition

#define UART_BUFFER_SIZE			128

#define UART_BAUDRATE				B115200

#define FRAME_HEADER				'+'
#define FRAME_ESCAPE				'/'


//Type definition


//Private variable definition

static uint m_ui_ReadLength = {0};
static uint8 m_u8_BufferWrite[UART_BUFFER_SIZE] = {0};
static uint8 m_u8_BufferRead[UART_BUFFER_SIZE] = {0};
static uint8 m_u8_BufferLog[UART_BUFFER_SIZE] = {0};
static drv_uart_callback m_t_Callback = {0};
static lib_frame_object m_t_Frame = {0};

static int m_i_FileDescriptor = -1;
static fd_set m_t_FileDescriptorSet = {0};

static const char m_i8_UARTFilePath[] = "/dev/ttyS2";


//Private function declaration


//Public function definition

uint DrvUART_Initialize(void)
{
	struct termios t_Config;


	if (m_i_FileDescriptor != -1)
	{
		close(m_i_FileDescriptor);
	}

	//Open UART device file in read and write mode
	m_i_FileDescriptor = open(m_i8_UARTFilePath,
		O_RDWR | O_NOCTTY | O_NONBLOCK);

	LOGD("Open UART device file %d", m_i_FileDescriptor);

	if (m_i_FileDescriptor == -1)
	{
		LOGE("Cannot open port");

		return FUNCTION_FAIL;
	}

	LOGD("Configuring serial port");

	//Get UART attributes
	if (tcgetattr(m_i_FileDescriptor, &t_Config))
	{
		LOGE("Get UART attributes failed");
		close(m_i_FileDescriptor);

		return FUNCTION_FAIL;
	}

	//Configure UART baud rate
	cfmakeraw(&t_Config);
	cfsetispeed(&t_Config, UART_BAUDRATE);
	cfsetospeed(&t_Config, UART_BAUDRATE);

	//Set UART attributes
	if (tcsetattr(m_i_FileDescriptor, TCSANOW, &t_Config))
	{
		LOGE("Set UART attributes failed");
		close(m_i_FileDescriptor);

		return FUNCTION_FAIL;
	}

	FD_ZERO(&m_t_FileDescriptorSet);
	FD_SET(m_i_FileDescriptor, &m_t_FileDescriptorSet);

	m_t_Frame.u8_Header = FRAME_HEADER;
	m_t_Frame.u8_Escape = FRAME_ESCAPE;
	LibFrame_Initialize(&m_t_Frame);

	return FUNCTION_OK;
}


void DrvUART_Finalize(void)
{
	LOGD("Close UART device file %d", m_i_FileDescriptor);

	if (m_i_FileDescriptor != -1)
	{
		close(m_i_FileDescriptor);
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
	uint ui_Value;


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
	lib_frame_int t_Length;
	char *u8p_BufferLog;


	LOGD("Write frame, length: %d", ui_Length);

	//Check if length of data is out of range
	if ((ui_Length <= 0) || (ui_Length > (UART_BUFFER_SIZE / 2) -
		LIB_FRAME_HEADER_LENGTH))
	{
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

	if (write(m_i_FileDescriptor, (const void *)m_u8_BufferWrite, t_Length) < 0)
	{
		LOGE("Write serial port fail");

		return FUNCTION_FAIL;
	}

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


	if (select(m_i_FileDescriptor + 1, &m_t_FileDescriptorSet, NULL ,NULL,
		NULL) > 0)
	{
		if (FD_ISSET(m_i_FileDescriptor, &m_t_FileDescriptorSet))
		{
			if (m_ui_ReadLength == 0)
			{
				t_Length = (lib_frame_int)read(m_i_FileDescriptor,
					(void *)&u8_Data, 1);
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
